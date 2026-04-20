#!/bin/bash
# BidHub Railway deploy script — run from repo root (cs4135_BidHub/)
set -e

echo "==> Fast-forwarding backend/main from development..."
cd backend
git checkout main
git merge --ff-only origin/development
git push origin main
git checkout development
cd ..

echo "==> Pinning backend SHA in root/main..."
git checkout main
git submodule update --remote --merge
SHA=$(git -C backend rev-parse --short HEAD)
git add backend
git commit -m "pin backend@${SHA}"
git push origin main
git checkout development

echo "==> Redeploying services to Railway..."
for svc in account-service auction-service admin-service api-gateway notification-service delivery-service; do
  echo "  deploying $svc..."
  railway up --service "$svc" --detach --path-as-root backend/
done

echo "==> Done. Live at https://api-gateway-production-d819.up.railway.app"
