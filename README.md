# sintef-grafter-gui-template

This is a suggestion for how the GUI SINTEF are building should
serialize Grafter pipelines.

See the file
[core.clj](https://github.com/dapaas/grafter-gui-template-pipeline/blob/master/src/sintef_grafter_gui_template/core.clj)
for the serialization.

## Notes

### Client & Server responsibilities

There must be a clean separation between client and server.  The
suggestion for the review is that the client is responsible for
generating the EDN (grafter code) and serializing it in the shape that
I have suggested here.

The final serialization as illustrated by
[core.clj](https://github.com/dapaas/grafter-gui-template-pipeline/blob/master/src/sintef_grafter_gui_template/core.clj)
should be submitted to the server as a single file.

The server will then need to be responsible for performing these tasks:

1. Placing the submitted file into the leiningen grafter project
structure also shown in this repository.
1. Packaging the transformation into an executable by running `lein uberjar`
   to generate the executable transformation.
1. Registering the transformation with Ontotexts Grafter service wrapper.

### File Order is important

The first thing to remember is that the order of definitions in the
serialization is important, as the file is read in one pass.  This
means that vars must be declared before they are used.  e.g. this is
ordering is allowed:

```clojure
(defn ->integer
  "An example transformation function that converts a string to an integer"
  [s]
  (Integer/parseInt s))

(defn pipeline [dataset]
  (-> dataset
      (mapc {:age ->integer})))
```

But this one isn't:

```clojure
(defn pipeline [dataset]
  (-> dataset
      (mapc {:age ->integer})))

(defn ->integer
  "An example transformation function that converts a string to an integer"
  [s]
  (Integer/parseInt s))

```

This is why user functions and prefixers are specified first.  There
are implications of this for user specified functions, as if one uses
another it must be defined before the other.  For this reason I
suggest users do not at this stage provide code for individual
functions, but provide code for all the functions they wish to use.

### Pipeline templates

I propose that an empty pipeline template should look like this:

```clojure
(defn pipeline [dataset]
  (-> dataset
      ;; <additional steps inserted here>
    ))
```

Steps should be added in order at the point indicated, e.g. a user
wants to drop two rows:

```clojure
(defn pipeline [dataset]
  (-> dataset
      (drop-rows 2) ;; <- inserted here
    ))
```

... Then a user wants to specify column headers (`make-dataset` can be
used for this)

```clojure
(defn pipeline [dataset]
  (-> dataset
      (drop-rows 2)
      (make-dataset [:person-uri :name :sex :age])  ;; <- inserted here
    ))
```

### Wiring up Graph Templates

The empty graph template for the above example should look like this:

```clojure
(def make-graph
  (graph-fn [{:keys [person-uri name sex age]}]
            ))
```

*NOTE* that the template needs to bind keys of the same name as used
 in the pipeline to symbols.  e.g. `:person-uri` `:name` `:sex` and
 `:age` from the pipeline require corresponding variable bindings in
 the template (`person-uri` `name` `sex` and `age` respectively).

This requirement means that the graph template needs to be updated
with respect to the transformation.  For example when a user changes
the pipeline the GUI will need to update the bindings in the template
accordingly.  This means re-processing the pipeline to look for calls
of the form `(make-dataset [:heading :names])` and also calls to
`(derive-column :new-column blah [:foo :bar])`.

This means if the user has a pipeline that looks like this:

```clojure
(defn pipeline [dataset]
  (-> dataset
      (drop-rows 2)
      (make-dataset [:person-uri :name :sex :age])  ;; <- inserted here
      (derive-column :new-column [:name :age] a-user-function)
    ))
```

That the empty graph template needs to become:

```clojure
(def make-graph
  (graph-fn [{:keys [person-uri name sex age new-column]}]
            ;; <graph clauses inserted here>
            ))
```
