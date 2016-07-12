public-radio-services
===============

Services for [publicradio.info](https://github.com/radioopensource/dotinfo)

To Run Locally:

1. Make sure [postgres](https://www.postgresql.org/download/) is installed.
2. Launch postgres by executing these commands:
	- `initdb pg`
	- `postgres -D pg &`
	- `createdb public_radio_services`
3. Add `jdbc:postgresql://localhost/public_radio_services` as the environment variable for DATABASE_URL.
4. Make sure [leiningen](http://leiningen.org/) is installed.
5. Run `lein with-profile dev ring server-headless`.
6. Endpoints should be live at localhost:3000.
