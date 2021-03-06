package com.yhongm.dynamic_core;

import android.util.Log;

import com.yhongm.dynamic_core.convert.JsonToBeanConvertFactory;

import org.json.JSONObject;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Created by yhongm on 2017/03/09.
 * 转换器
 */

public interface Converter<R, T> {//R为需要转换的类型，T为转换后的类型

    T conver(R value) throws IOException;

    abstract class Factory {
        public Converter<?, String> inputConverter(Type type, Annotation[] annotations, Dynamic dynamic) {
            return null;
        }

        public JsonToBeanConvertFactory.JsonToBeanConverter<ExecuteResponse<JSONObject>> resultConverter(Type type, Annotation[] annotations, Dynamic dynamic) {
            Log.i("Factory", "15:21/resultConverter:");// yhongm 2017/03/13 15:21
            return null;
        }

        public Converter<?, ?> responseBodyConverter(Type type, Annotation[] annotations, Dynamic dynamic) {
            return null;
        }

        public Converter<?, ?> requestBodyConverter(Type type, Annotation[] annotations, Dynamic dynamic) {
            return null;
        }
    }
}
