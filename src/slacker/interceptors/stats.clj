(ns slacker.interceptors.stats
  (:use [slacker.interceptor])
  (:require [clojure.contrib.jmx :as jmx])
  (:import [clojure.contrib.jmx Bean])
  (:import [javax.management DynamicMBean MBeanInfo
            MBeanAttributeInfo Attribute AttributeList]))

(def stats-data (atom {}))

(defn assoc-with-default [m k]
  (update-in m [k] #(if (nil? %) 1 (inc %))))

(definterceptor function-call-stats
  :before (fn [req]
            (when (nil? (:code req))
              (let [fname (:fname req)]
                (swap! stats-data assoc-with-default fname)))
            req))

(def stats-bean
  (reify
    DynamicMBean
    (getAttribute [this key]
      (@stats-data key))
    (getAttributes [this keys]
      (AttributeList.
       (apply list (map #(Attribute. % (get @stats-data %)) keys))))
    (getMBeanInfo [this]
      (MBeanInfo. (.. @stats-data getClass getName)
                  "slacker function call statistics"
                  (into-array
                   MBeanAttributeInfo
                   (map #(MBeanAttributeInfo. %
                                              "java.lang.Integer"
                                              "function invocation count"
                                              true
                                              false
                                              false)
                        (keys @stats-data)))
                  nil
                  nil
                  nil))
    (invoke [this action params sig])
    (setAttribute [this attr])
    (setAttributes [this attrs])))

(jmx/register-mbean stats-bean "slacker.server:type=FunctionCallStats")

