#!/usr/bin/env bash
#
# One-time copy of the DigitalOcean Spaces bucket into the R2 bucket, using rclone.
#
# Reads from packages/server/.env.spaces (the DigitalOcean key, kept apart because it is temporary):
#   AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_S3_ENDPOINT, AWS_S3_BUCKET
# and from packages/server/.env (see .env.template):
#   R2_ACCOUNT_ID, R2_ACCESS_KEY_ID, R2_SECRET_ACCESS_KEY
#
# Usage:
#   ./migrate-from-spaces.sh            # dry run, shows what would be copied
#   ./migrate-from-spaces.sh --copy     # copy, then verify with rclone check
#   ./migrate-from-spaces.sh --mirror   # the other way: copy R2 into Spaces, so embeds on old runtime
#                                       # versions (which read the Spaces bucket directly) keep working
#
set -euo pipefail
cd "$(dirname "$0")"

set -a
# shellcheck disable=SC1091
source ../.env
# shellcheck disable=SC1091
source ../.env.spaces
set +a

: "${AWS_ACCESS_KEY_ID:?}" "${AWS_SECRET_ACCESS_KEY:?}" "${AWS_S3_ENDPOINT:?}" "${AWS_S3_BUCKET:?}"
: "${R2_ACCOUNT_ID:?}" "${R2_ACCESS_KEY_ID:?}" "${R2_SECRET_ACCESS_KEY:?}"

# Remotes are defined through the environment, so nothing is written to ~/.config/rclone.
export RCLONE_CONFIG_DO_TYPE=s3
export RCLONE_CONFIG_DO_PROVIDER=DigitalOcean
export RCLONE_CONFIG_DO_ACCESS_KEY_ID="$AWS_ACCESS_KEY_ID"
export RCLONE_CONFIG_DO_SECRET_ACCESS_KEY="$AWS_SECRET_ACCESS_KEY"
export RCLONE_CONFIG_DO_ENDPOINT="$AWS_S3_ENDPOINT"

export RCLONE_CONFIG_R2_TYPE=s3
export RCLONE_CONFIG_R2_PROVIDER=Cloudflare
export RCLONE_CONFIG_R2_ACCESS_KEY_ID="$R2_ACCESS_KEY_ID"
export RCLONE_CONFIG_R2_SECRET_ACCESS_KEY="$R2_SECRET_ACCESS_KEY"
export RCLONE_CONFIG_R2_ENDPOINT="https://${R2_ACCOUNT_ID}.r2.cloudflarestorage.com"
export RCLONE_CONFIG_R2_ACL=private

SRC="do:${AWS_S3_BUCKET}"
DST="r2:nodeboxlive"

if [[ "${1:-}" == "--mirror" ]]; then
  rclone copy "$DST" "$SRC" --progress --transfers 16 --checkers 32 --s3-no-check-bucket --s3-acl public-read
  echo
  echo "Verifying..."
  rclone check "$DST" "$SRC" --one-way --checkers 32
  echo "Mirror verified: every R2 object exists in Spaces with the same size and hash."
elif [[ "${1:-}" == "--copy" ]]; then
  rclone copy "$SRC" "$DST" --progress --transfers 16 --checkers 32 --s3-no-check-bucket
  echo
  echo "Verifying..."
  rclone check "$SRC" "$DST" --one-way --checkers 32
  echo "Copy verified: every source object exists in R2 with the same size and hash."
else
  rclone copy "$SRC" "$DST" --dry-run --transfers 16 --checkers 32 --s3-no-check-bucket 2>&1 | tail -5
  echo
  echo "Dry run only. Re-run with --copy to perform the copy."
fi
