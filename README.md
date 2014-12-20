# Easy Does It

Client-side Standalone TODO

[Live Demo](https://cdn.rawgit.com/runexec/Easy-Does-It/30fa5a87a9640216cb5ddf1671e264550e36edcf/STANDALONE/index.html)

![alt text](preview/intro.png "Preview Intro Image")


Animation of the program.

![alt text](preview/preview.gif "Preview GIF Image")

Screen responsive layout.

![alt text](preview/responsive.png "Preview Responsive Image")

# Download

Standalone version is available as STANDALONE.zip .

# TODO

* Change the icons from WTF to something that doesn't suck
* Add server detection
* Add server aware specific features
* Slap on some versioning information somewhere
* Async calls to prevent blocks during large renders

# Version 2 Preview

![alt text](preview/v2.mobile.preview.png "Preview Responsive Image")

![alt text](preview/v2.preview.png "Preview Responsive Image")

## Development

Start a REPL (in a terminal: `lein repl`, or from Emacs: open a
clj/cljs file in the project, then do `M-x cider-jack-in`. Make sure
CIDER is up to date).

In the REPL do

```clojure
(run)
(browser-repl)
```

The call to `(run)` does two things, it starts the webserver at port
10555, and also the Figwheel server which takes care of live reloading
ClojureScript code and CSS. Give them some time to start.

Running `(browser-repl)` starts the Weasel REPL server, and drops you
into a ClojureScript REPL. Evaluating expressions here will only work
once you've loaded the page, so the browser can connect to Weasel.

When you see the line `Successfully compiled "resources/public/app.js"
in 21.36 seconds.`, you're ready to go. Browse to
`http://localhost:10555` and enjoy.

**Attention: It is not longer needed to run `lein figwheel`
  separately. This is now taken care of behind the scenes**

## Deploying to Heroku

This assumes you have a
[Heroku account](https://signup.heroku.com/dc), have installed the
[Heroku toolbelt](https://toolbelt.heroku.com/), and have done a
`heroku login` before.

``` sh
git init
git add -A
git commit
heroku create
git push heroku master:master
heroku open
```

## Running with Foreman

Heroku uses [Foreman](http://ddollar.github.io/foreman/) to run your
app, which uses the `Procfile` in your repository to figure out which
server command to run. Heroku also compiles and runs your code with a
Leiningen "production" profile, instead of "dev". To locally simulate
what Heroku does you can do:

``` sh
lein with-profile -dev,+production uberjar && foreman start
```

Now your app is running at
[http://localhost:5000](http://localhost:5000) in production mode.

## License

Copyright © 2014 Ryan Kelker

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
