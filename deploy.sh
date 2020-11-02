#! /usr/bin/env sh
set -e

echo "## Compiling"
npx shadow-cljs release frontend

echo "## Uploading"
rsync -e 'ssh -p 9125' -avzP ./public/ root@176.9.1.25:/instant-website-frontend

REV=$(git rev-parse HEAD)

curl -XPOST https://instantwebsite.grafana.net/api/annotations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ANNOTATE_API_TOKEN" \
  --data @- << EOF
  {
    "text": "Deployed frontend@$REV",
    "tags": [
      "deployment", "frontend"
    ]
  }
EOF

echo "DONE!"
