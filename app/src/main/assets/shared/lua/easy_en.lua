--[[
easy_en: 内嵌英文翻译器支持——在小鹤方案内做英文模糊匹配（* 通配）与英文造词。
独立 easy_en 方案与 ecdict 释义已随 single-schema 移除：英文候选不再带中文注释。
--]]

local easy_en = {}

function easy_en.init(env)
    local config = env.engine.schema.config
    env.tag = config:get_string("easy_en/tag") or "easy_en"
    env.prompt = config:get_string("easy_en/tips") or "英文"
    env.wildcard = config:get_string("easy_en/wildcard") or "*"
    -- Memory 绑定当前方案的 easy_en 内嵌段（原为独立 easy_en 方案的 translator 段）。
    env.mem = Memory(env.engine, env.engine.schema, "easy_en")
    env.expand_word_count = config:get_int("easy_en/word_search_limit") or 666
end

function easy_en.fini(env)
    if env.mem then
        env.mem:disconnect()
        env.mem = nil
        collectgarbage('collect')
    end
end

function easy_en.translator(input, seg, env)
    local engine = env.engine
    local composition = engine.context.composition
    if composition:empty() then
        return
    end
    local segment = composition:back()
    if segment:has_tag(env.tag) or input:match("^%l+%*%l+$") then
        segment.prompt = "〔" .. env.prompt .. "〕"
        local tailer = string.match(input, "[^" .. env.wildcard .. "]+$") or ""
        local header = string.match(input, "^[^" .. env.wildcard .. "]+")
        env.mem:dict_lookup(header, true, env.expand_word_count) -- expand_search
        for dictentry in env.mem:iter_dict() do
            local code_tail = string.match(dictentry.comment:lower(), tailer .. "$") or ""
            if tailer and (code_tail == tailer) then
                local ph = Phrase(env.mem, "expand_en_word", seg.start, seg._end, dictentry)
                ph.comment = dictentry.comment:lower()
                yield(ph:toCandidate())
            end
        end
    end
end

return {
    translator = { init = easy_en.init, func = easy_en.translator, fini = easy_en.fini },
}
