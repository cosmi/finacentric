(ns finacentric.mail)



(defn send-reg-token! [email reg-token]
  (println "Użytkowniku" email ", zarejestruj się, korzystając z następującego kodu:" reg-token))