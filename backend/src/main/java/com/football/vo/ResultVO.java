package com.football.vo;

import lombok.Data;

@Data
public class ResultVO<T> {

    private Integer code;

    private String msg;

    private T data;

    public static <T> ResultVO<T> success(T data) {
        ResultVO<T> result = new ResultVO<>();
        result.setCode(0);
        result.setMsg("success");
        result.setData(data);
        return result;
    }

    public static <T> ResultVO<T> success() {
        return success(null);
    }

    public static <T> ResultVO<T> error(String msg) {
        ResultVO<T> result = new ResultVO<>();
        result.setCode(-1);
        result.setMsg(msg);
        return result;
    }

    public static <T> ResultVO<T> error(Integer code, String msg) {
        ResultVO<T> result = new ResultVO<>();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }
}
