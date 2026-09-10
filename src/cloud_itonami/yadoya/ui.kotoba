(ns cloud-itonami.yadoya.ui
  "View tree for the yadoya (yadoya-ui-b7r4n2xq) appview. Ported 1:1 from
  the former appview/yadoya-ui-b7r4n2xq/svelte/src/routes/+page.svelte
  (hotel search screen: region/city/max-JPY filters, XRPC searchHotels
  result list, loading/error/empty states). Structural chrome comes from
  appkit.core / kotoba-ui.core (murakumo-studio構成); interactive controls
  are hand-rolled hiccup styled with kotoba-ui's exposed class-name,
  mirroring cloud-itonami.app-itonami.ui/btn and
  cloud-itonami.public-malak.ui."
  (:require [appkit.core :as shape]
            [kotoba-ui.core :as ui]
            [cloud-itonami.yadoya.state :as state]))

(def css-text
  "
.yad-app { min-height: 100vh; padding: 24px; background: var(--liquid-glass-bg, #f8fafc); color: var(--liquid-glass-fg, #0f172a); font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, \"Segoe UI\", sans-serif; }
.yad-main { max-width: 56rem; margin: 0 auto; display: grid; gap: 1.5rem; }
.yad-top h1 { font-size: 1.5rem; font-weight: 700; margin: 0 0 .25rem; }
.yad-top p { font-size: .875rem; color: #4b5563; margin: 0; }
.yad-form { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: .75rem; border: 1px solid #e5e7eb; border-radius: 8px; padding: 1rem; }
@media (max-width: 640px) { .yad-form { grid-template-columns: 1fr; } }
.yad-field { display: flex; flex-direction: column; gap: .25rem; font-size: .875rem; }
.yad-field input, .yad-field select { border: 1px solid #d1d5db; border-radius: 6px; padding: .25rem .5rem; font: inherit; background: #fff; color: inherit; }
.yad-actions { display: flex; align-items: flex-end; }
.yad-submit { width: 100%; padding: .5rem 1rem; border-radius: 6px; font: inherit; }
.yad-error { border: 1px solid #fecaca; background: #fef2f2; border-radius: 6px; padding: .75rem; font-size: .875rem; color: #991b1b; }
.yad-count { font-size: .875rem; color: #4b5563; margin: 0 0 .5rem; }
.yad-list { list-style: none; margin: 0; padding: 0; border: 1px solid #e5e7eb; border-radius: 8px; overflow: hidden; }
.yad-list > li { display: flex; flex-direction: column; gap: .25rem; padding: .75rem; border-bottom: 1px solid #f3f4f6; }
.yad-list > li:last-child { border-bottom: 0; }
@media (min-width: 640px) { .yad-list > li { flex-direction: row; align-items: center; justify-content: space-between; } }
.yad-hotel-name { font-weight: 500; margin: 0; }
.yad-hotel-meta { font-size: .75rem; color: #6b7280; margin: 0; }
.yad-source { font-size: .875rem; color: #1d4ed8; text-decoration: underline; }
.yad-empty { padding: 1rem; font-size: .875rem; color: #6b7280; }
")

(defn- search-form []
  (let [{:keys [form loading?]} @state/state
        {:keys [region city price-jpy-max]} form]
    [:form.yad-form
     {:on-submit (fn [e] (.preventDefault e) (state/search!))}
     [:label.yad-field "Region"
      [:select {:value region
                :on-change #(state/set-form! :region (.. % -target -value))}
       [:option {:value ""} "(any)"]
       (for [r ["asia" "europe" "mena" "americas" "africa" "oceania"]]
         ^{:key r} [:option {:value r} r])]]
     [:label.yad-field "City"
      [:input {:value city
               :placeholder "Tokyo"
               :on-change #(state/set-form! :city (.. % -target -value))}]]
     [:label.yad-field "Max JPY / night"
      [:input {:type "number" :min "0"
               :value (or price-jpy-max "")
               :on-change #(state/set-form! :price-jpy-max
                                            (let [v (.. % -target -value)]
                                              (if (= "" v) nil (js/Number v))))}]]
     [:div.yad-actions
      [:button.yad-submit {:class (ui/class-name :button)
                           :type "submit"
                           :disabled loading?}
       (if loading? "Searching…" "Search")]]]))

(defn- hotel-row [{:keys [vertex_id name city country isic_code osm_id source_url] :as h}]
  ^{:key vertex_id}
  [:li
   [:div
    [:p.yad-hotel-name name]
    [:p.yad-hotel-meta
     (str (when city (str city " ")) (or country "") " · " isic_code
          (when osm_id (str " · OSM " osm_id)))]]
   (when source_url
     [:a.yad-source {:href source_url :target "_blank" :rel "noopener"} "source"])])

(defn- results []
  (let [{:keys [hotels total loading?]} @state/state]
    [:section
     [:p.yad-count total " hotel(s)"]
     (if (seq hotels)
       [:ul.yad-list (doall (map hotel-row hotels))]
       (when-not loading?
         [:ul.yad-list [:li.yad-empty "No hotels match. Try widening the filter."]]))]))

(defn root []
  (let [{:keys [error]} @state/state]
    [:div
     [:style css-text]
     [shape/panel
      [:main.yad-app
       [:div.yad-main
        [:header.yad-top
         [:h1 "yadoya.etzhayyim.com"]
         [:p "Hotel search & reservation — ISIC I5510 catalog, ADR-0036 Worker-direct, bridged to "
          [:code "did:web:hospitality.etzhayyim.com"] "."]]
        [search-form]
        (when error [:div.yad-error error])
        [results]]]]]))
