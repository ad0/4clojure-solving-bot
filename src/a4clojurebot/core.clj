(ns a4clojurebot.core
  (:require [clj-http.client :as client]))

;; parameters
(def login  "*********")
(def passwd "*********")
(def verbose false) ; to show the submitted code

;; global defs
(def urlbase "http://www.4clojure.com")
(def login-url (str urlbase "/login"))
(def probs-url (str urlbase "/problems"))
(defn prob-url
  [n]
  (str urlbase "/problem/" n))

;; regexp for parsing problem data
(def probline (str "<tr class=\\\"(?:even|odd)row\\\">"
                   "<td class=\\\"titlelink\\\">"
                   "<a href=\\\"/problem/(\\d+)\\\">"
                   "([^<]+)"
                   "</a></td>"
                   "<td class=\\\"centered\\\">"
                   "(Elementary|Easy|Medium|Hard)"
                   "</td>"
                   "<td class=\\\"centered\\\">"
                   "([^<]*)"
                   "</td>"
                   "<td class=\\\"centered\\\">"
                   "([^<]+)"
                   "</td>"
                   "<td class=\\\"centered\\\">"
                   "(?:\\d+)"
                   "</td>"
                   "<td class=\\\"centered\\\">"
                   "<img alt=\\\""
                   "(incomplete|completed)"
                   "\\\""))

(defn parse-problems
  [content]
  (let [l (re-seq (re-pattern probline) content)]
    (loop [l l problems []]
      (if (empty? l)
        problems
        (let [[[_ id title difficulty topics author solved?] & l] l]
          (recur l (conj problems {:id id
                                   :title title
                                   :difficulty difficulty
                                   :topics topics
                                   :author author
                                   :solved? (= solved? "completed")})))))))

(defn parse-test-cases
  [content]
  (map (comp first rest)
       (re-seq #"<pre class=\"test\">([^<]+)</pre>" content)))

(defn submit
  "try a request of a code for a given problem"
  [problem strcode]
  (when verbose (println (str "[*] submitting code: " strcode)))
  (let [formdata {:form-params {:code strcode :id (str (:id problem))}}
        ;; first request is to post code
        content  (:body (client/post (prob-url (:id problem)) formdata))
        ;; second request is to retrieve result
        content  (:body (client/get (prob-url (:id problem))))]

    (when (re-find #"Congratulations" content)
      (throw (Exception. "problem solved")))))

(defn strify
  "output code in a way so that special types are correctly handled"
  [obj]
  (cond
    (nil? obj) "nil"
    (= (type obj) java.lang.String) (str "\"" obj "\"")
    :else (str obj)))

(defn build-subst
  "attempt to simply subst '__' with expected result on the first test case"
  [test-cases]
  (let [tcase (first test-cases)]
    (cond
      (and (= '= (nth tcase 0)) (= '__ (nth tcase 1))) [(str (nth tcase 2))]
      (and (= '= (nth tcase 0)) (= '__ (nth tcase 2))) [(str (nth tcase 1))]
      :else [])))

(defn str-case
  "with given parameters and expected result, build a line of a cond"
  [given expected]
  (let [gs (loop [gs given n 1 acc []]
             (if (empty? gs)
               acc
               (let [[g & gs] gs
                     s (if (nil? g)
                         "true"
                         (str "(= arg" n " " (strify g) ")"))]
                 (recur gs (inc n) (conj acc s)))))]
    (str "(and " (clojure.string/join " " gs) ") " (strify expected))))

(defn str-fn
  "given cases as strings and # of args, return the magic function"
  [nb-args cases]
  (let [args (map #(str "arg" %) (take nb-args (iterate (partial + 1) 1)))]
    (str "(fn [" (clojure.string/join " " args) "]\n"
         "(cond\n"
         (clojure.string/join "\n" cases)
         "\n" "))")))

(defn build-funs
  "build a function that enumarates test cases params with expected results"
  [test-cases]
  (let [tcase (first test-cases)]
    (try
      (cond
        (try (and (= '= (first tcase)) (= '__ (-> tcase rest ffirst)))
             (catch Exception e false))
        (let [nb-args (-> tcase rest first rest count)]
          [(let [cases (map (fn [tcase]
                             (let [expected (last tcase)
                                   args (-> tcase rest first rest)]
                               (str-case (concat (repeat (dec nb-args) nil)
                                                 [(last args)]) expected)))
                           test-cases)]
             (str-fn nb-args cases)),
           (let [cases (map (fn [tcase]
                             (let [expected (last tcase)
                                   args (-> tcase rest first rest)]
                               (str-case (map #(nth args %) (range nb-args)) expected)))
                           test-cases)]
             (str-fn nb-args cases))])
  
        (try (and (= '= (first tcase)) (= '__ (-> tcase rest rest ffirst)))
             (catch Exception e false))
        (let [nb-args (-> tcase rest rest first rest count)]
          [(let [cases (map (fn [tcase]
                             (let [expected (first (rest tcase))
                                   args (-> tcase rest rest first rest)]
                               (str-case (concat (repeat (dec nb-args) nil)
                                                 [(last args)]) expected)))
                           test-cases)]
             (str-fn nb-args cases)),
           (let [cases (map (fn [tcase]
                           (let [expected (first (rest tcase))
                                 args (-> tcase rest rest first rest)]
                             (str-case (map #(nth args %) (range nb-args)) expected)))
                         test-cases)]
            (str-fn nb-args cases))])

        :else [])
      (catch Exception e []))))

(defn try-to-solve
  "a function that attempts to automatically solve a 4clojure problem"
  [problem]
  (let [content (-> problem :id prob-url client/get :body)
        test-cases (map read-string (parse-test-cases content))]
    (try
      (doseq [code (concat (build-subst test-cases)
                           (build-funs  test-cases))]
        (submit problem code))
      ;; exception is raised when a problem is solved
      (catch Exception e true))))


(defn -main
  [& args]

  ;; set up the cookie store
  (binding [clj-http.core/*cookie-store* (clj-http.cookies/cookie-store)]

    ;; log in
    (client/post login-url {:form-params {:user login 
                                          :pwd  passwd}})

    ;; get the list of challenges
    (let [content (clojure.string/replace (:body (client/get probs-url))
                                          #"(<span class='tag'>)|(</span>)"
                                          "")
          problems (parse-problems content)
          unsolved (filter (comp not :solved?) problems)]
      (println (str "[*] just retrieved " (count problems) " problems "
                    "(" (count unsolved) " are unsolved)"))

      (def counter (atom 0N))

      ;; time to try to break some problems!
      (doseq [problem problems]
        (println (str "[*] trying to solve problem " (:id problem)
                      " (\"" (:title problem) "\", " (:difficulty problem) ")"))
        (if (try-to-solve problem)
          ;; it worked!
          (do
            (swap! counter inc)
            (println "[+] OK! solved :)"))
          ;; oops..
          (println "[-] I have to learn more clojure...")))

      ;; a final word
      (println (str "[*] I was able to solve " @counter " problems!")))))
