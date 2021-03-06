;; First we need to start with a basic namespace definition to import
;; most of the functions we'll need.

(ns sintef-grafter-gui-template.core
  (:require
   [grafter.rdf :refer [graph-fn graph s add prefixer]]
   [grafter.tabular :refer [column-names columns rows all-columns derive-column
                            mapc swap drop-rows open-all-datasets make-dataset
                            move-first-row-to-header _]]
   [grafter.rdf.sesame :as ses]
   [grafter.rdf.ontologies.rdf :refer :all]
   [grafter.rdf.ontologies.foaf :refer :all]
   [grafter.rdf.ontologies.void :refer :all]
   [grafter.rdf.ontologies.dcterms :refer :all]
   [grafter.rdf.ontologies.vcard :refer :all]
   [grafter.rdf.ontologies.pmd :refer :all]
   [grafter.rdf.ontologies.qb :refer :all]
   [grafter.rdf.ontologies.os :refer :all]
   [grafter.rdf.ontologies.sdmx-measure :refer :all]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; PREFIXERS & USER FUNCTIONS
;;
;; At the top of the file we should include all the users custom
;; functions and their prefixers as both of these are used in both
;; pipelines and graph-templates.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def base-domain (prefixer "http://my-domain.com"))

(def base-graph (prefixer (base-domain "/graph/")))

(def base-id (prefixer (base-domain "/id/")))

(def base-vocab (prefixer (base-domain "/def/")))

(def base-data (prefixer (base-domain "/data/")))

;; ...

;; User functions

(defn ->integer
  "An example transformation function that converts a string to an integer"
  [s]
  (Integer/parseInt s))

;; ...

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Graph Template
;;
;; Next up we should include the graph template as it used by the pipeline.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def make-graph
  (graph-fn [{:keys [name sex age person-uri gender]}]
            (graph (base-graph "example")
                   [person-uri
                    [rdf:a foaf:Person]
                    [foaf:gender sex]
                    [foaf:age age]
                    [foaf:name (s name)]])))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Pipeline
;;
;; Next we should include the GUI specified pipeline
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn pipeline [dataset]
  (-> dataset
      (drop-rows 1)
      (make-dataset [:name :sex :age])
      (derive-column :person-uri [:name] base-id)
      (mapc {:age ->integer
             :sex {"f" (s "female")
                   "m" (s "male")}})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Finally we can include some boiler plate to run this from the
;; command line start it.  This code should probably not be considered
;; part of the template, and should not be generated by the front end,
;; but right now it is necessary to run it.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn import-data
  [quads-seq destination]
  (add (ses/rdf-serializer destination) quads-seq))

(defn -main [& [path output]]
  (when-not (and path output)
    (println "Usage: lein run <input-file.csv> <output-file.(nt|rdf|n3|ttl)>")
    (System/exit 0))

  (-> (open-all-datasets path)
      first
      pipeline
      make-graph
      (import-data output))

  (println path "=>" output))
