-- KEYS: {queue, pass}
    -- 쿠폰의 대기열: waiting:{couponId}:queue
    -- 해당 사용자의 입장권: waiting:{couponId}:pass:{userId}
-- ARGV: userId
-- 반환: {status, position}
    -- status 0=대기, 1=통과,
    -- position 0=통과, 1>=대기 순번

if redis.call('EXISTS', KEYS[2]) == 1 then
  return {'1', '0'}
end

-- score = 들어온 시각. Redis TIME 이라 서버가 여러 대여도 단일 시계로 FIFO 보장.
local t = redis.call('TIME')
-- 마이크로초 단위로 변환
local score = tonumber(t[1]) * 1000000 + tonumber(t[2])
-- NX: 이미 줄에 있으면 그대로 두어 새로고침해도 순번이 안 밀린다.
-- 대기열에 사용자 ID를 등록한다.
-- 정렬 기준은 등록 시각이다.
-- 이미 등록된 사용자라면 변경하지 않는다.
redis.call('ZADD', KEYS[1], 'NX', score, ARGV[1])

local rank = redis.call('ZRANK', KEYS[1], ARGV[1])
return {'0', tostring(rank + 1)}