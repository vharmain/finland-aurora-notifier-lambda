#!/bin/bash

export AWS_ACCESS_KEY_ID=FILL
export AWS_SECRET_ACCESS_KEY=FILL
export AWS_REGION=eu-west-1
export AWS_DEFAULT_REGION=$AWS_REGION

export SCRAPE_URL=http://aurorasnow.fmi.fi/public_service/magforecast_fi.html
export STATION=Hankasalmi # see url above for list of stations
export DELIVERY_METHOD=email # (email|sms)

export EMAIL_RECIPIENT=mail@example.com
export EMAIL_SENDER=mail@example.com

export SMS_PHONE_NUMBER=+358400123123
export SMS_SENDER_ID=AURORA # max 11 alphanumeric characters
