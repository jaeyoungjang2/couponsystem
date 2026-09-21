#!/usr/bin/env bash
# v2 초과발급 시나리오 통합 러너.
#
# 사용:
#   ./scripts/load/v2/run.sh
#
# reset -> create_coupon -> k6 -> verify 를 한 번에 실행한다.

set -euo pipefail
cd "$(dirname "$0")/../../.."

printf '\n\033[1;36m===== v2 초과발급 시나리오 =====\033[0m\n'
./scripts/load/v2/reset.sh

# 쿠폰 생성 및 id 반환
coupon_id="$(./scripts/load/v2/create_coupon.sh)"
# 쿠폰 과발급 테스트
k6 run -e COUPON_ID="$coupon_id" scripts/load/v2/over_issuance.js
# 결과 확인
COUPON_ID="$coupon_id" ./scripts/load/v2/verify.sh