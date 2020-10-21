#! /usr/bin/env sh
set -e

echo "## Compiling"
npx shadow-cljs release frontend

echo "## Uploading"
rsync -e 'ssh -p 9125' -avzP ./public/ root@176.9.1.25:/instant-website-frontend

echo "DONE!"
