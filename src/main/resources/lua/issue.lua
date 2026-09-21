-- 1 args로 key를 받음
local remaining = tonumber(redis.call('GET', KEYS[1]) or 0)
if remaining <= 0 then
    -- 실패 응답 반환
    return 0
end
-- 재고를 차감
redis.call('DECR', KEYS[1])
-- 성공 응답 반환
return 1