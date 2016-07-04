public-radio-services
===============

Services for [publicradio.info](https://github.com/radioopensource/dotinfo)

To Run Locally:

1. Make sure postgres is installed
2. Launch postgres by executing these commands:
	- `initdb pg`
	- `postgres -D pg &`
	- `createdb public_radio_services`
3. Add `jdbc:postgresql://localhost/public_radio_services` as the environment variable for DATABASE_URL 