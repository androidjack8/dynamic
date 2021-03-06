package com.yhongm.dynamic_core;

import com.yhongm.dynamic_core.convert.JsonToBeanConvertFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.yhongm.dynamic_core.Utils.checkNotNull;

/**
 * Created by yhongm on 2017/03/08.
 */

public class Dynamic {
    private static Map<Method, ClassMethod<?, ?>> clsMethodCache = new ConcurrentHashMap<>();
    private final ArrayList<Converter.Factory> convertFactorys;
    private final ArrayList<ResultAdapter.Factory> callAdapterFactorys;
    private HandleResult handleResult;

    public Dynamic(ArrayList<Converter.Factory> converterFactorys, ArrayList<ResultAdapter.Factory> callAdapterFactorys, HandleResult handleResult) {
        this.convertFactorys = converterFactorys;
        this.callAdapterFactorys = callAdapterFactorys;
        this.handleResult = handleResult;
    }

    public Dynamic(ArrayList<Converter.Factory> converterFactorys, ArrayList<ResultAdapter.Factory> callAdapterFactorys) {
        this.convertFactorys = converterFactorys;
        this.callAdapterFactorys = callAdapterFactorys;
    }

    public ResultAdapter<?, ?> callAdapter(Type returnType, Annotation[] annotations) {
        return nextCallAdapter(null, returnType, annotations);
    }

    private ResultAdapter<?, ?> nextCallAdapter(ResultAdapter.Factory skipPast, Type returnType, Annotation[] annotations) {

        int start = callAdapterFactorys.indexOf(skipPast) + 1;
        for (int i = start, count = callAdapterFactorys.size(); i < count; i++) {
            ResultAdapter<?, ?> resultAdapter = callAdapterFactorys.get(i).get(returnType, annotations, this);
            if (resultAdapter != null) {
                return resultAdapter;
            }
        }
        throw new IllegalArgumentException("");
    }

    public Converter responseBodyConverter(Type responseType, Annotation[] annotations) {
        return nextResponseBodyConverter(null, responseType, annotations);
    }

    private Converter nextResponseBodyConverter(Converter.Factory skipPast, Type responseType, Annotation[] annotations) {
        checkNotNull(responseType, "type==null");
        checkNotNull(annotations, "annotation==null");
        int start = convertFactorys.indexOf(skipPast) + 1;
        for (int i = start, count = convertFactorys.size(); i < count; i++) {
            Converter<?, ?> converter = convertFactorys.get(i).resultConverter(responseType, annotations, this);
            if (converter != null) {
                return converter;
            }
        }
        throw new IllegalArgumentException("没有找到匹配的转换器");
    }

    public <T> Converter<T, String> stringConverer(Type parameterType, Annotation[] parameterAnnotations) {

        for (int i = 0, count = convertFactorys.size(); i < count; i++) {
            Converter<?, String> converter = convertFactorys.get(i).inputConverter(parameterType, parameterAnnotations, this);
            if (converter != null) {
                return (Converter<T, String>) converter;
            }
        }
        return (Converter<T, String>) DefaultConverter.ToStringConverters.mInstance;
    }

    public static class Builder {
        ArrayList<Converter.Factory> converterFactorys = new ArrayList<>();
        ArrayList<ResultAdapter.Factory> callAdapterFactorys = new ArrayList<>();
        private HandleResult handleResult = null;

        public Builder() {
            AndroidMainThreadExecutor androidPlatform = new AndroidMainThreadExecutor();
            converterFactorys.add(new DefaultConverter());
            converterFactorys.add(JsonToBeanConvertFactory.create());
            callAdapterFactorys.add(new ExecutorResultAdapteFactory(androidPlatform.getExecutor()));
        }

        public void addConvertFactory(Converter.Factory factory) {
            converterFactorys.add(factory);
        }

        public Builder handleResult(HandleResult result) {
            this.handleResult = result;
            return this;
        }

        public Dynamic build() {
            if (handleResult == null) {
                return new Dynamic(converterFactorys, callAdapterFactorys);
            } else {
                return new Dynamic(converterFactorys, callAdapterFactorys, handleResult);
            }

        }
    }

    public <T> T create(Class<T> cls) {
        Utils.validateServiceInterface(cls);

        return (T) Proxy.newProxyInstance(cls.getClassLoader(), new Class[]{cls}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.getDeclaringClass() == Object.class) {
                    method.invoke(this, args);
                }
                ClassMethod<Object, Object> classMethod = (ClassMethod<Object, Object>) loadClsMethod(method);
                HandleResult result;
                if (handleResult != null) {

                    result = handleResult;
                    result.setArgs(classMethod, args);
                } else {
                    result = new HandleResult<>();
                    result.setArgs(classMethod, args);
                }
                Object adapter = classMethod.resultAdapter.adapter(result);
                return adapter;
            }
        });
    }

    private ClassMethod<?, ?> loadClsMethod(Method method) {
        ClassMethod<?, ?> classMethod = clsMethodCache.get(method);
        if (classMethod != null) {
            return classMethod;
        }
        synchronized (clsMethodCache) {
            classMethod = clsMethodCache.get(method);
            if (classMethod == null) {
                classMethod = new ClassMethod.Builder(this, method).build();
                clsMethodCache.put(method, classMethod);
            }
        }
        return classMethod;
    }
}
