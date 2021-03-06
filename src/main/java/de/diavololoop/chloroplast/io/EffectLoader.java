package de.diavololoop.chloroplast.io;

import de.diavololoop.chloroplast.effect.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Chloroplast
 *
 * Loader for Effects. Read Effects from programroot/effects/.
 */
public class EffectLoader {

    File effectDir;
    private Map effects = new HashMap<Effect, Class<?>>();

    /**
     * creates an EffectLoader with the give root directory
     *
     * @param root the directory in which the effects directory is located
     */
    public EffectLoader(File root){

        effectDir = new File(root, "effects");
        if(effectDir.isFile()){
            throw new RuntimeException("can't create directory "+effectDir.getAbsolutePath()+" - a file with the same name already exists");
        }
        if(!effectDir.exists()){
            boolean success = effectDir.mkdirs();

            if(!success) {
                throw new RuntimeException("can't create directory "+effectDir.getAbsolutePath());
            }
        }

    }

    /**
     * map of all currently loaded effects (effectname -> effect)
     *
     * @return map of  effects
     */
    public Map<String, Effect> allEffects(){
        return effects;
    }


    /**
     * drop all loaded effects and reload effects from effect directory
     */
    public void loadEffects(){

        effects.clear();

        List<File> files = new LinkedList<File>();
        indexEffects(effectDir, files);

        files.stream().filter(file -> file.getName().endsWith(".java")).forEach(this::compileEffect);
        files.stream().filter(file -> file.getName().endsWith(".stream")).forEach(this::loadStreamedEffect);
        files.stream().filter(file -> file.getName().endsWith(".class")).forEach(this::loadCompiledEffect);

        Effect basicColor = new EffectSimpleColor();
        Effect rainbow = new EffectRainbow();
        Effect blinker = new EffectRandomBlink();
        Effect queue = new EffectQueue();
        Effect spectrum = new EffectSpectrum();
        Effect colorwave = new EffectColorwave();

        effects.put(basicColor.getName(), basicColor);
        effects.put(rainbow.getName(), rainbow);
        effects.put(blinker.getName(), blinker);
        effects.put(queue.getName(), queue);
        effects.put(spectrum.getName(), spectrum);
        effects.put(colorwave.getName(), colorwave);


    }

    private void loadStreamedEffect(File file) {

        try {
            EffectWrapperStream effect = new EffectWrapperStream(file);
            effects.put(effect.getName(), effect);
        } catch (Exception e) {
            System.err.println("cant load stream effect "+file.getName()+": "+e.getMessage());
            e.printStackTrace();
        }

    }

    private void indexEffects(File root, List<File> result){

        File[] files = root.listFiles();
        for (File f: files){
            if(f.isDirectory()){
                indexEffects(f, result);
            }else{
                result.add(f);
            }
        }

    }

    private void compileEffect(File file){
        try {

            Process p = Runtime.getRuntime().exec("javac "+file.getAbsolutePath());
            p.waitFor();
            int exitCode = p.exitValue();


            if(exitCode != 0){
                System.err.println("error while compiling effect: "+file.getName());
            }

        } catch (IOException e) {
            System.err.println("error while compiling effect: " + file.getName()+"\r\ncaused by:\r\n" + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void loadCompiledEffect(File file){
        String className = file.getAbsolutePath()
                .replace('\\', '/')
                .replaceFirst(effectDir.getAbsolutePath().replace('\\', '/'), "")
                .replaceAll("/", ".");
        className = className.substring(0, className.lastIndexOf('.')).substring(1);

        try {

            URLClassLoader classLoader = URLClassLoader.newInstance(new URL[] { effectDir.toURI().toURL() });
            Class<?> cls = Class.forName(className, true, classLoader);


            if(Effect.class.isAssignableFrom(cls)){
                Effect e = (Effect)cls.newInstance();
                effects.put(e.getName(), e);
            }else{
                EffectWrapperClass effect = new EffectWrapperClass(cls);
                effects.put(effect.getName(), effect);
            }



        } catch (Exception e) {//im so sorry for that
            System.err.println("cant load compiled effect "+className+": "+e.getMessage());
        }
    }

}
