package cn.yusiwen.nettymvc.core.handler;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import cn.yusiwen.nettymvc.core.model.Message;
import cn.yusiwen.nettymvc.session.Session;

/**
 * @author yusiwen
 */
@SuppressWarnings("unchecked")
public abstract class AbstractHandler {

    /**
     * Message flag
     */
    public static final int MESSAGE = 0;
    /**
     * Session flag
     */
    public static final int SESSION = 1;

    /**
     * Target object
     */
    protected final Object targetObject;
    /**
     * Target method
     */
    protected final Method targetMethod;
    /**
     * Parameter types
     */
    protected final int[] parameterTypes;
    /**
     * Return void or not
     */
    protected final boolean returnVoid;
    /**
     * Async or not
     */
    protected final boolean async;

    /**
     * Description
     */
    protected final String desc;

    public AbstractHandler(Object targetObject, Method targetMethod, String desc) {
        this(targetObject, targetMethod, desc, false);
    }

    public AbstractHandler(Object targetObject, Method targetMethod, String desc, boolean async) {
        this.targetObject = targetObject;
        this.targetMethod = targetMethod;
        this.returnVoid = targetMethod.getReturnType().isAssignableFrom(Void.TYPE);
        this.async = async;
        if (desc == null || desc.isEmpty()) {
            desc = targetMethod.getName();
        }
        this.desc = desc;

        Type[] paraTypes = targetMethod.getGenericParameterTypes();
        int[] types = new int[paraTypes.length];
        try {
            for (int i = 0; i < paraTypes.length; i++) {
                Type type = paraTypes[i];
                Class<?> clazz = (Class<?>) type;

                if (Message.class.isAssignableFrom(clazz)) {
                    types[i] = MESSAGE;
                } else if (Session.class.isAssignableFrom(clazz)) {
                    types[i] = SESSION;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.parameterTypes = types;
    }

    public <T extends Message> T invoke(T request, Session session) throws Exception {
        Object[] args = new Object[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            int type = parameterTypes[i];
            switch (type) {
                case AbstractHandler.MESSAGE:
                    args[i] = request;
                    break;
                case AbstractHandler.SESSION:
                    args[i] = session;
                    break;
                default:
                    break;
            }
        }
        return (T) targetMethod.invoke(targetObject, args);
    }

    public boolean isReturnVoid() {
        return returnVoid;
    }

    public boolean isAsync() {
        return async;
    }

    public String getDesc() {
        return desc;
    }

    @Override
    public String toString() {
        return desc;
    }
}
