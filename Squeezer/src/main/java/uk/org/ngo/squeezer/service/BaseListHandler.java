package uk.org.ngo.squeezer.service;

import android.support.v4.app.Fragment.InstantiationException;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.util.Reflection;
import uk.org.ngo.squeezer.framework.Item;

abstract class BaseListHandler<T extends Item> implements ListHandler<T> {
    private static final String TAG = BaseListHandler.class.getSimpleName();

    protected List<T> items;

    @SuppressWarnings("unchecked")
    private final Class<T> dataType = (Class<T>) Reflection
            .getGenericClass(this.getClass(), ListHandler.class, 0);

    private Constructor<T> constructor;

    @Override
    public Class<T> getDataType() {
        return dataType;
    }

    @Override
    public List<T> getItems() {
        return items;
    }

    @Override
    public void clear() {
        items = new ArrayList<T>() {
            private static final long serialVersionUID = 1321113152942485275L;
        };
    }

    @Override
    public void add(Map<String, String> record) {
        if (constructor == null) {
            try {
                constructor = dataType.getDeclaredConstructor(Map.class);
            } catch (Exception e) {
                throw new InstantiationException(
                        "Unable to create constructor for " + dataType.getName(), e);
            }
        }
        try {
            items.add(constructor.newInstance(record));
        } catch (Exception e) {
            throw new InstantiationException("Unable to create new " + dataType.getName(), e);
        }
    }

}
