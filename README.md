# Easy Does It 2 

Optionally server backed, color coded, task management with a percentage overview.

## Download and Run

All available versions can be downloaded at =>

https://github.com/runexec/Easy-Does-It

Using Git =>

`git clone https://github.com/runexec/Easy-Does-It`

If you choose to run the server, you can do so by running =>

`lein run -m easy-does-it.server/run`

Older versions are still available at =>

https://github.com/runexec/Easy-Does-It/releases

## Watch and Use

[Watch Demo Video](https://raw.githubusercontent.com/runexec/Easy-Does-It/master/readme/videos/preview.ogv)

Desktop

![alt Desktop 1.png][readme/images/d1.png]

Mobile

![alt Mobile 1.png][readme/images/m1.png]

Desktop

![alt Desktop 2.png][readme/images/d2.png]

Mobile

![alt Mobile 2.png][readme/images/m2.png]

Desktop

![alt Desktop 3.png][readme/images/d3.png]

Mobile

![alt Mobile 3.png][readme/images/m3.png]

Desktop

![alt Desktop 4.png][readme/images/d4.png]

Mobile

![alt Mobile 4.png][readme/images/m4.png]

Desktop

![alt Desktop 5.png][readme/images/d5.png]

Mobile

![alt Mobile 5.png][readme/images/m5.png]

## Development

Open a terminal and type `lein repl` to start a Clojure REPL
(interactive prompt).

In the REPL, type

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
Leiningen "uberjar" profile, instead of "dev". To locally simulate
what Heroku does you can do:

``` sh
lein with-profile -dev,+uberjar uberjar && foreman start
```

Now your app is running at
[http://localhost:5000](http://localhost:5000) in production mode.

## License

Copyright © 2014 Ryan Kelker

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

## Chestnut

Created with [Chestnut](http://plexus.github.io/chestnut/) 0.7.0-SNAPSHOT.
