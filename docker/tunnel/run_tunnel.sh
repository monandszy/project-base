#!/run/current-system/sw/bin/bash

docker exec --user=0 prod-tunnel-1 \
    cloudflared tunnel --config /etc/cloudflared/conf.yml \
    run app-tunnel