#!/bin/bash

API_TOKEN="token"
ZONE_ID="1b533c899a41135beeae3b171aac19d2"
DOMAIN_NAME="monand.tech"

RECORD=$(curl -X GET "https://api.cloudflare.com/client/v4/zones/$ZONE_ID/dns_records?name=$DOMAIN_NAME" \
     -H "Authorization: Bearer $API_TOKEN")

echo "${RECORD}"

RECORD_ID=$(echo "$RECORD" | jq -r '.result[0].id')

echo "EXTRACTED: ${RECORD_ID}"

curl -X DELETE "https://api.cloudflare.com/client/v4/zones/$ZONE_ID/dns_records/$RECORD_ID" \
     -H "Authorization: Bearer $API_TOKEN" \
     -H "Content-Type: application/json"