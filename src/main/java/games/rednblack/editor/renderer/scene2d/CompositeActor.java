package games.rednblack.editor.renderer.scene2d;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import games.rednblack.editor.renderer.data.*;
import games.rednblack.editor.renderer.resources.IResourceRetriever;
import games.rednblack.editor.renderer.scripts.IActorScript;

import java.util.Arrays;
import java.util.HashMap;


/**
 * Created by azakhary on 7/26/2015.
 */
public class CompositeActor extends Group {

    protected IResourceRetriever ir;

    private float pixelsPerWU;
    private float resMultiplier;

    protected CompositeItemVO vo;
    private Array<IActorScript> scripts = new Array<IActorScript>(3);
    private HashMap<Integer, Actor> indexes = new HashMap<Integer, Actor>();
    private HashMap<String, LayerItemVO> layerMap = new HashMap<String, LayerItemVO>();

    public CompositeActor(CompositeItemVO vo, IResourceRetriever ir) {
        this(vo, ir, BuiltItemHandler.DEFAULT);
    }

    public CompositeActor(CompositeItemVO vo, IResourceRetriever ir, BuiltItemHandler itemHandler) {
        this(vo, ir, itemHandler, true);
    }

    private CompositeActor(CompositeItemVO vo, IResourceRetriever ir, BuiltItemHandler itemHandler, boolean isRoot) {
        this.ir= ir;
        this.vo = vo;

        pixelsPerWU = ir.getProjectVO().pixelToWorld;

        ResolutionEntryVO resolutionEntryVO = ir.getLoadedResolution();
        resMultiplier = resolutionEntryVO.getMultiplier(ir.getProjectVO().originalResolution);

        makeLayerMap(vo);
        build(vo, itemHandler, isRoot);
    }

    private void makeLayerMap(CompositeItemVO vo) {
        layerMap.clear();
        for(int i = 0; i < vo.layers.size; i++) {
            layerMap.put(vo.layers.get(i).layerName,vo.layers.get(i));
        }
    }

    protected void build(CompositeItemVO vo, BuiltItemHandler itemHandler, boolean isRoot) {
        buildImages(vo.getElementsArray(SimpleImageVO.class), itemHandler);
        build9PatchImages(vo.getElementsArray(Image9patchVO.class), itemHandler);
        buildLabels(vo.getElementsArray(LabelVO.class), itemHandler);
        buildComposites(vo.getElementsArray(CompositeItemVO.class), itemHandler);
        processZIndexes();
        recalculateSize();

        if(isRoot) {
            buildCoreData(this, vo);
            itemHandler.onItemBuild(this);
        }
    }

    protected void buildComposites(Array<CompositeItemVO> composites, BuiltItemHandler itemHandler) {
        for(int i = 0; i < composites.size; i++) {
            String className   =   getClassName(composites.get(i).customVariables);
            CompositeActor actor;
            if(className!=null){
                try {
                    Class<?> c = ClassReflection.forName(className);
                    actor   =   (CompositeActor) ClassReflection.getConstructors(c)[0].newInstance(composites.get(i), ir, itemHandler);
                }catch (Exception ex){
                    actor  = new CompositeActor(composites.get(i), ir, itemHandler, false);
                }
            }else {
                actor  = new CompositeActor(composites.get(i), ir, itemHandler, false);
            }
            processMain(actor, composites.get(i));
            addActor(actor);

            itemHandler.onItemBuild(actor);
        }
    }

    private String getClassName(ObjectMap<String, String> customVars) {
        String className = customVars.get("className");
        if(className != null && className.equals("")){
            className   =   null;
        }
        return className;
    }

    public void addScript(IActorScript iScript) {
        scripts.add(iScript);
        iScript.init(this);
    }

    protected void buildImages(Array<SimpleImageVO> images, BuiltItemHandler itemHandler) {
        for(int i = 0; i < images.size; i++) {
            Image image = new Image(ir.getTextureRegion(images.get(i).imageName));
            processMain(image, images.get(i));
            addActor(image);

            itemHandler.onItemBuild(image);
        }
    }

    protected void build9PatchImages(Array<Image9patchVO> patches, BuiltItemHandler itemHandler) {
        for(int i = 0; i < patches.size; i++) {
            TextureAtlas.AtlasRegion region = (TextureAtlas.AtlasRegion) ir.getTextureRegion(patches.get(i).imageName);
            int[] splits = region.findValue("split");
            NinePatch ninePatch = new NinePatch(region, splits[0], splits[1], splits[2], splits[3]);
            Image image = new Image(ninePatch);
            image.setWidth(patches.get(i).width*pixelsPerWU/resMultiplier);
            image.setHeight(patches.get(i).height * pixelsPerWU/resMultiplier);
            processMain(image, patches.get(i));
            addActor(image);

            itemHandler.onItemBuild(image);
        }
    }

    protected void buildLabels(Array<LabelVO> labels, BuiltItemHandler itemHandler) {
        for(int i = 0; i < labels.size; i++) {
            Label.LabelStyle style = new Label.LabelStyle(ir.getBitmapFont(labels.get(i).style, labels.get(i).size, labels.get(i).monoSpace), Color.WHITE);
            Label label = new Label(labels.get(i).text, style);
            label.setAlignment(labels.get(i).align);
            label.setWidth(labels.get(i).width * pixelsPerWU / resMultiplier);
            label.setHeight(labels.get(i).height * pixelsPerWU / resMultiplier);
            processMain(label, labels.get(i));
            addActor(label);

            itemHandler.onItemBuild(label);
        }
    }

    protected void processMain(Actor actor, MainItemVO vo) {

        actor.setName(vo.itemIdentifier);
        buildCoreData(actor, vo);

        //actor properties
        actor.setPosition(vo.x * pixelsPerWU/resMultiplier, vo.y * pixelsPerWU/resMultiplier);
        actor.setOrigin(vo.originX * pixelsPerWU/resMultiplier, vo.originY * pixelsPerWU/resMultiplier);
        actor.setScale(vo.scaleX, vo.scaleY);
        actor.setRotation(vo.rotation);
        actor.setColor(new Color(vo.tint[0], vo.tint[1], vo.tint[2], vo.tint[3]));

        indexes.put(getLayerIndex(vo.layerName) + vo.zIndex, actor);

        if(layerMap.get(vo.layerName).isVisible) {
            actor.setVisible(true);
        } else {
            actor.setVisible(false);
        }
    }

    protected void buildCoreData(Actor actor, MainItemVO vo){
        //core data
        CoreActorData data = new CoreActorData();
        data.id = vo.itemIdentifier;
        data.layerIndex = getLayerIndex(vo.layerName);
        data.tags = vo.tags;
        data.customVariables.putAll(vo.customVariables);

        actor.setUserObject(data);
    }


    protected void processZIndexes() {
        Object[] indexArray = indexes.keySet().toArray();
        Arrays.sort(indexArray);

        for(int i = 0; i < indexArray.length; i++) {
            indexes.get(indexArray[i]).setZIndex(i);
        }
    }

    public int getLayerIndex(String name) {
        return vo.layers.indexOf(layerMap.get(name), false);
    }

    public Actor getItem(String id) {
        for(Actor actor: getChildren()) {
            Object userObject = actor.getUserObject();
            if(userObject != null && userObject instanceof CoreActorData
                    && (id.equals(((CoreActorData) userObject).id))) {
                return actor;
            }
        }
        return null;
    }

    public void recalculateSize() {
        float lowerX = 0, lowerY = 0, upperX = 0, upperY = 0;
        for (int i = 0; i < getChildren().size; i++) {
            Actor value = getChildren().get(i);
            if (i == 0) {
                if (value.getScaleX() > 0 && value.getWidth() * value.getScaleX() > 0) {
                    lowerX = value.getX();
                    upperX = value.getX() + value.getWidth() * value.getScaleX();
                } else {
                    upperX = value.getX();
                    lowerX = value.getX() + value.getWidth() * value.getScaleX();
                }

                if (value.getScaleY() > 0 && value.getHeight() * value.getScaleY() > 0) {
                    lowerY = value.getY();
                    upperY = value.getY() + value.getHeight() * value.getScaleY();
                } else {
                    upperY = value.getY();
                    lowerY = value.getY() + value.getHeight() * value.getScaleY();
                }
            }
            if (value.getScaleX() > 0 && value.getWidth() > 0) {
                if (lowerX > value.getX()) lowerX = value.getX();
                if (upperX < value.getX() + value.getWidth() * value.getScaleX())
                    upperX = value.getX() + value.getWidth() * value.getScaleX();
            } else {
                if (upperX < value.getX()) upperX = value.getX();
                if (lowerX > value.getX() + value.getWidth() * value.getScaleX())
                    lowerX = value.getX() + value.getWidth() * value.getScaleX();
            }
            if (value.getScaleY() > 0 && value.getHeight() * value.getScaleY() > 0) {
                if (lowerY > value.getY()) lowerY = value.getY();
                if (upperY < value.getY() + value.getHeight() * value.getScaleY())
                    upperY = value.getY() + value.getHeight() * value.getScaleY();
            } else {
                if (upperY < value.getY()) upperY = value.getY();
                if (lowerY > value.getY() + value.getHeight() * value.getScaleY())
                    lowerY = value.getY() + value.getHeight() * value.getScaleY();
            }

        }

        setWidth(upperX);
        setHeight(upperY);
    }

    public void setLayerVisibility(String layerName, boolean isVisible) {
        final int layerIndex = getLayerIndex(layerName);
        layerMap.get(layerName).isVisible = isVisible;

        for(Actor actor: getChildren()) {
            Object userObject = actor.getUserObject();
            if(userObject != null && userObject instanceof CoreActorData
                    && ((CoreActorData)userObject).layerIndex == layerIndex) {
                actor.setVisible(isVisible);
            }
        }
    }

    /**
     * get's list of children that contain a specified tag.
     * Does not yet go in depth.
     *
     * @param tag
     * @return
     */
    public Array<Actor> getItemsByTag(String tag) {
        Array<Actor> items = new Array<Actor>();
        for(Actor actor: getChildren()) {
            Object userObject = actor.getUserObject();
            if(userObject != null && userObject instanceof CoreActorData) {
                CoreActorData data = (CoreActorData) userObject;
                if(data.tags != null && Arrays.asList(data.tags).contains(tag))
                    items.add(actor);
            }
        }

        return items;
    }

    /**
     * returns children of this actor that are on specified layer
     * @param layerName
     * @return
     */
    public Array<Actor> getItemsByLayer(String layerName) {
        final int layerIndex = getLayerIndex(layerName);
        Array<Actor> items = new Array<Actor>();

        for(Actor actor: getChildren()) {
            Object userObject = actor.getUserObject();
            if(userObject != null && userObject instanceof CoreActorData
                    && ((CoreActorData)userObject).layerIndex == layerIndex) {
                items.add(actor);
            }
        }
        return items;
    }
    
    public Array<IActorScript> getScripts() {
        return scripts;
    }

    public CompositeItemVO getVo() {
        return vo;
    }

    /**
     * Enables customization of the CompositeActor during building.
     * Example use cases: tag handling, custom variables handling
     */
    public interface BuiltItemHandler {

        BuiltItemHandler DEFAULT = new BuiltItemHandler() {
            @Override
            public void onItemBuild(Actor item) {

                if(item instanceof CompositeActor) {
                    CoreActorData data = (CoreActorData) item.getUserObject();
                    if(data != null && data.tags != null && Arrays.asList(data.tags).contains("button"))
                        item.addListener(new ButtonClickListener());
                }
            }
        };

        /**
         * @param item newly built and added to a parent (in case it's not a root actor)
         */
        void onItemBuild(Actor item);

    }

    @Override
    public void act(float delta) {
        for (int i = 0; i < scripts.size; i++) {
            scripts.get(i).act(delta);
        }
        super.act(delta);
    }
}
