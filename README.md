# Finland Aurora Notifier Lambda

AWS Lambda web-scraper which sends notification to pre-defined email/sms address if the [Finnish Meteorological Institute Aurora Forecast](http://aurorasnow.fmi.fi/public_service/magforecast_en.html) shows promising activity at selected observation station.

Read more information about the [forecast](http://aurorasnow.fmi.fi/public_service/forecast_description_en.html).

The code is written for personal use.

## Install Dependencies

```shell
$ lein deps
```

## SES email setup

You need to [verify](https://docs.aws.amazon.com/ses/latest/DeveloperGuide/verify-email-addresses-procedure.html) sender and recipient email addresses in order to send email through SES. If you want to send emails to non-verified recipients you need to [move out of the sandbox](https://docs.aws.amazon.com/ses/latest/DeveloperGuide/request-production-access.html).

## Set environment variables

``` shell
$ cp .env.sample.sh .env.sh

# Fill in config variables
$ $EDITOR .env.sh
```

## Deploy

```shell
$ source .env.sh
$ serverless deploy
```

## Set Invoke Schedule

You can't see the auroras if the sky is not dark. Therefore it makes sense to trigger the check only during the aurora season and limit checks to evenings and nights.

See [AWS Lambda docs on cron syntax](https://docs.aws.amazon.com/AmazonCloudWatch/latest/events/ScheduledEvents.html) and edit trigger in `serverless.yml` accordingly.
