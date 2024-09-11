#!/run/current-system/sw/bin/bash

docker exec -e TUNNEL_ORIGIN_CERT=/etc/cloudflared/cert.pem \
    --user=0 networking-cloudflared \
    cloudflared tunnel \
    --config /etc/cloudflared/conf.yml \
    run app-tunnel