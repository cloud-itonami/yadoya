(ns cloud-itonami.yadoya.state
  "App state for the yadoya (yadoya-ui-b7r4n2xq) hotel-search appview UI.
  Ported 1:1 from the former
  appview/yadoya-ui-b7r4n2xq/svelte/src/routes/+page.svelte and its
  src/lib/yadoya-xrpc.ts — a single screen: region / city / max-JPY filters
  driving an XRPC com.etzhayyim.apps.yadoya.searchHotels query, with
  loading / error / empty states and a hotel result list. Single reagent
  atom, murakumo-studio構成."
  (:require [clojure.string :as str]
            [reagent.core :as r]))

(defonce state
  (r/atom
   {:form       {:region "asia" :city "" :price-jpy-max nil}
    :hotels     []
    :total      0
    :loading?   false
    :error      nil}))

(defn- service []
  ;; mirrors yadoya-xrpc.ts service(): window override, then same-origin on
  ;; etzhayyim.com, then the pipethrough default.
  (let [w        js/window
        loc      (.-location w)
        override (.-__YADOYA_SERVICE__ w)]
    (cond
      (and (string? override) (not (str/blank? override)))
      override

      (and (not (contains? #{"localhost" "127.0.0.1"} (.-hostname loc)))
           ^Boolean (str/ends-with? (.-origin loc) "etzhayyim.com"))
      (.-origin loc)

      :default
      "https://atproto.etzhayyim.com")))

(defn- qs
  "URLSearchParams string from a map, skipping nil / empty values
  (yadoya-xrpc.ts: `if (v === undefined || v === null || v === '') continue`)."
  [m]
  (->> m
       (remove (fn [[_ v]] (or (nil? v) (identical? "" v))))
       (map (fn [[k v]]
              (str (js/encodeURIComponent (name k)) "="
                   (js/encodeURIComponent (str v)))))
       (str/join "&")))

(defn search! []
  (let [{:keys [region city price-jpy-max]} (:form @state)
        query (qs {:region region
                   :city city
                   :priceJpyMax price-jpy-max
                   :limit 50})
        url   (str (service)
                   "/xrpc/com.etzhayyim.apps.yadoya.searchHotels"
                   (when-not (str/blank? query) (str "?" query)))]
    (swap! state assoc :loading? true :error nil)
    (-> (js/fetch url #js {:headers #js {:accept "application/json"}})
        (.then (fn [^js resp]
                 (if (.-ok resp)
                   (.json resp)
                   (.then (.text resp)
                          (fn [^js text]
                            (throw (js/Error.
                                     (str "searchHotels " (.-status resp) ": " text))))))))
        (.then (fn [^js out]
                 (let [parsed (js->clj (or (.-hotels out) #js [])
                                       :keywordize-keys true)
                       hotels (mapv #(select-keys % [:vertex_id :name :city
                                                      :country :isic_code
                                                      :osm_id :source_url])
                                    parsed)]
                   (swap! state assoc
                          :hotels    hotels
                          :total      (int (or (.-total out) (count hotels)))
                          :loading?   false
                          :error     nil))))
        (.catch (fn [^js err]
                  (swap! state assoc
                         :hotels  []
                         :total   0
                         :loading? false
                         :error    (.-message err)))))))

(defn set-form! [k v]
  (swap! state assoc-in [:form k] v))
