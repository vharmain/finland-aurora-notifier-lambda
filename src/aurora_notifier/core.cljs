(ns aurora-notifier.core
  (:require
   ["aws-sdk" :as AWS]
   ["cheerio" :as cheerio]
   ["node-fetch" :as node-fetch]
   [goog.object :as gobj]
   [goog.string :as gstring]
   [goog.string.format]))

(def sns (new AWS/SNS))
(def ses (new AWS/SES))

(def thresholds
  {"#00FF00" "Low (green)"
   "#FFFF00" "Raised (yellow)"
   "#FFA500" "Moderate (orange)"
   "#FF0000" "Strong (red)"
   "#800000" "Severe (brown)"})

;; Sensitivity can be adjusted here by selecting different codes.
(def interesting (-> thresholds
                     (select-keys ["#FFA500" "#FF0000" "#800000"])
                     keys
                     set))

(defn ->message [{:keys [station color]}]
  (let [threshold (get thresholds color)]
    (gstring/format "%s aurora action in %s!" threshold station)))

(defn send-sms [{:keys [phone-number sender-id] :as context}]
  (let [message (->message context)
        params  {:Message           message
                 :PhoneNumber       phone-number
                 :MessageAttributes {"AWS.SNS.SMS.SenderID"
                                     {:DataType    "String"
                                      :StringValue sender-id}}}]
    (-> sns
        (.publish (clj->js params))
        .promise
        (.then #(assoc context :result %)))))

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
        .promise
        (.then #(assoc context :result %)))))

(defn notify [{:keys [method] :as context}]
  (println "Interesting action detected. Notifying via" method)
  (case method
    "email" (send-email context)
    "sms"   (send-sms context)
    (throw (js/Error. (gstring/format "Unkonwn delivery method '%'" method)))))

(defn maybe-notify [{:keys [station colors] :as context}]
  (if-let [color (some interesting colors)]
    (notify (merge context {:color color}))
    {:result (gstring/format "Nothing going on in %s" station)}))

(defn parse [{:keys [html station] :as context}]
  (let [$      (.load cheerio html)
        colors (-> ($ (gstring/format "tr:contains('%s')" station))
                   .children
                   (.map (fn [_i el] (.attr ($ el) "bgcolor")))
                   (.get))]
    (js/Promise.resolve (assoc context :colors colors))))

(defn fetch-data [{:keys [url] :as context}]
  (-> (node-fetch url)
      (.then #(merge context {:html %}))))

(defn check-auroras* [context]
  (-> (fetch-data context)
      (.then parse)
      (.then maybe-notify)
      (.then (comp println :result))))

(defn check-auroras [_event _ctx]
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
