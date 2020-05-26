# Guestbook App

I created this web app while following the book [Web Development with Clojure, Third Edition](https://pragprog.com/book/dswdcloj3/web-development-with-clojure-third-edition).

The following libraries are used:
* Http-kit for the web server
* Ring for web server abstraction
* Mount for managing stateful components
* Reitit for routing
* Selmer for server-side rendering
* Muuntaja for HTTP format endcoding and decoding
* Struct for shared schema validation on the front-end and back-end
* Reagent and Re-frame for the front-end framework
* Sente for WebSockets

## Running ##

To start a web server for the application, run:

    lein run
