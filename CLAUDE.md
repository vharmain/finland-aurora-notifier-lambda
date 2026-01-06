# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AWS Lambda that scrapes the Finnish Meteorological Institute Aurora Forecast and sends notifications (email via SES or SMS via SNS) when aurora activity reaches configured thresholds at a selected observation station.

## Tech Stack

- **ClojureScript** compiled with **shadow-cljs** targeting Node.js 20 (ARM64)
- **AWS SAM** for Lambda deployment
- **AWS SDK v3** (@aws-sdk/client-ses, @aws-sdk/client-sns)

## Build & Deploy Commands

```shell
# Install dependencies
npm install

# Build release (compiles ClojureScript to target/main.js)
npm run build

# Start shadow-cljs watch for development
npm run watch

# Deploy to AWS Lambda (builds + deploys, requires source .env.sh first)
source .env.sh && npm run deploy

# Test locally with Node.js
source .env.sh && node -e "require('./target/main.js').check({}, {})"
```

## Environment Setup

Copy `.env.sample.sh` to `.env.sh`, fill in values, then `source .env.sh` before deploying.

Required environment variables:
- `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`
- `SCRAPE_URL` - FMI forecast page URL
- `STATION` - observation station name (e.g., "Hankasalmi")
- `DELIVERY_METHOD` - "email" or "sms"
- Email: `EMAIL_RECIPIENT`, `EMAIL_SENDER`
- SMS: `SMS_PHONE_NUMBER`, `SMS_SENDER_ID`

## Architecture

Single namespace `aurora-notifier.core` with a promise-based pipeline:

1. `check-auroras` - Lambda entry point, reads config from environment
2. `fetch-data` - fetches FMI forecast HTML
3. `parse` - uses cheerio to extract aurora activity colors for the station
4. `maybe-notify` - checks if any color matches "interesting" thresholds
5. `notify` → `send-email` or `send-sms` - sends notification via AWS SDK

The `thresholds` map defines activity levels by color code. The `interesting` set (orange, red, brown) determines which levels trigger notifications.

## Lambda Configuration

- Handler: `target/main.check` (exported as `:check` in shadow-cljs)
- Runtime: Node.js 20.x on ARM64 architecture
- Schedule defined in `template.yaml` - runs every 15 min during evening hours (18-23 UTC) in aurora season months (Jan-Apr, Sep-Dec)
