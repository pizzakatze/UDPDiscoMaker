package de.diavololoop.chloroplast.effect;

import de.diavololoop.chloroplast.color.ColorModel;
import de.diavololoop.chloroplast.config.SpacePosition;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by gast2 on 26.09.17.
 */
public class EffectWrapperClass extends Effect{

    private String name;
    private String author;
    private String description;
    private int preferedFPS;

    private Method initMethod;
    private Method updateMethod;

    private Object instance;

    public EffectWrapperClass(Class<?> cls) throws IllegalAccessException, InstantiationException, InvocationTargetException {
        this.instance = cls.newInstance();

        Method[] methods = cls.getDeclaredMethods();
        for(Method m: methods){

            if(m.getName().equals("getName")){
                Object result = m.invoke(instance);
                if(result instanceof String){
                    name = (String)result;
                }
            }else if(m.getName().equals("getAuthor")){
                Object result = m.invoke(instance);
                if(result instanceof String){
                    author = (String)result;
                }
            }else if(m.getName().equals("getDescription")){
                Object result = m.invoke(instance);
                if(result instanceof String){
                    description = (String)result;
                }
            }else if(m.getName().equals("getPreferedFPS")){
                Object result = m.invoke(instance);
                if(result instanceof Integer){
                    preferedFPS = (int)result;
                }else{
                    System.err.println("Effect getPreferedFPS not return int: "+cls.getName());
                }
            }else if(m.getName().equals("init")){
                Class<?>[] types = m.getParameterTypes();
                if(types.length == 2 && types[0] == String.class && types[1] == List.class){
                    initMethod = m;
                }else{
                    throw new IllegalArgumentException("init method must match init(String args, List<byte[]> positions)");
                }
            }else if(m.getName().equals("update")){
                if(!String.class.isAssignableFrom(m.getReturnType())){
                    throw new IllegalArgumentException("update method must return String");
                }
                Class<?>[] types = m.getParameterTypes();
                if(types.length == 3 && types[0] == long.class && types[1] == int.class && types[2].isArray() && types[2].getComponentType() == byte.class){
                    updateMethod = m;
                }else{
                    throw new IllegalArgumentException("update method must match update(long time, int step, byte[] data)");
                }
            }

        }

    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getAuthor() {
        return author;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public int getPreferedFPS() {
        return preferedFPS;
    }

    @Override
    public void init(String args, List<SpacePosition> positions) {
        try {
            initMethod.invoke(instance, args, positions.stream().map(pos -> new float[]{pos.x, pos.y, pos.z}).collect(Collectors.toList()));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ColorModel update(long time, int step, byte[] data) {
        try {
            return ColorModel.getModel((String)updateMethod.invoke(instance, time, step, data));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void kill() {
    }

    @Override
    public String toString(){
        return "effect{name="+getName()+", author="+getAuthor()+", description="+getDescription()+", fps="+getPreferedFPS()+", class="+this.getClass().getName()+"}";
    }
}
