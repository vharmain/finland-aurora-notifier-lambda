(ns aurora-notifier.core
  (:require [cljs-lambda.macros :refer-macros [deflambda]]
            [cljs.nodejs :as nodejs]
            [goog.object :as gobj]
            [goog.string :as gstring]
            [goog.string.format]
            [promesa.core :as p]))

(def rp (nodejs/require "request-promise"))
(def AWS (nodejs/require "aws-sdk"))
(def cheerio (nodejs/require "cheerio"))

(def sns (new AWS.SNS))
(def ses (new AWS.SES))

(def strengths
  {"#00FF00" "Low (green)"
   "#FFFF00" "Raised (yellow)"
   "#FFA500" "Moderate (orange)"
   "#FF0000" "Strong (red)"
   "#800000" "Severe (brown)"})

;; Sensitivity can be adjusted here by selecting different codes.
(def interesting (-> strengths
                     (select-keys ["#FFA500" "#FF0000" "#800000"])
                     keys
                     set))

(defn parse [{:keys [station] :as context} html]
  (let [$      (.load cheerio html)
        colors (-> ($ (gstring/format "tr:contains('%s')" station))
                   .children
                   (.map (fn [i el] (.attr ($ el) "bgcolor")))
                   (.get))]
    (p/promise (merge context {:colors colors
                               :html   html}))))

(defn ->message [{:keys [station color]}]
  (let [strength (get strengths color)]
    (gstring/format "%s aurora action in %s!" strength station)))

(defn send-sms [{:keys [phone-number sender-id] :as context}]
  (let [message (->message context)
        params  {:Message           message
                 :PhoneNumber       phone-number
                 :MessageAttributes {"AWS.SNS.SMS.SenderID"
                                     {:DataType    "String"
                                      :StringValue sender-id}}}]
    (-> sns
        (.publish (clj->js params))
        .promise)))

(defn send-email [{:keys [email email-sender html] :as context}]
  (let [message (->message context)
        charset "UTF-8"
        params  {:Destination {:ToAddresses [email]}
                 :Message
                 {:Subject {:Charset charset :Data message}
                  :Body    {:Html {:Charset charset :Data html}
                            :Text {:Charset charset :Data (pr-str context)}}}
                 :Source email-sender}]
    (-> ses
        (.sendEmail (clj->js params))
        .promise)))

(defn notify [{:keys [method] :as context}]
  (println "Interesting action detected. Notifying via" method)
  (case method
    "email" (send-email context)
    "sms"   (send-sms context)
    (throw (js/Error. (gstring/format "Unkonwn delivery method" method)))))

(defn maybe-notify [{:keys [station colors] :as context}]
  (if-let [color (some interesting colors)]
    (notify (merge context {:color color}))
    (gstring/format "Nothing going on in %s" station)))

(defn check-auroras* [{:keys [url] :as context}]
  (-> (rp url)
      (p/then (partial parse context))
      (p/then maybe-notify)
      (p/then println)))

(deflambda check-auroras [event ctx]
  (let [context
        {:url          (gobj/get js/process.env "SCRAPE_URL")
         :station      (gobj/get js/process.env "STATION")
         :method       (gobj/get js/process.env "DELIVERY_METHOD")
         :phone-number (gobj/get js/process.env "SMS_PHONE_NUMBER")
         :sender-id    (gobj/get js/process.env "SMS_SENDER_ID")
         :email        (gobj/get js/process.env "EMAIL_RECIPIENT")
         :email-sender (gobj/get js/process.env "EMAIL_SENDER")}]
    (println "Checking if there's aurora activity in" (:station context))
    (check-auroras* context)))
