package games.rednblack.editor.renderer.factory.v2;

import com.artemis.ComponentMapper;
import com.artemis.EntityTransmuter;
import com.artemis.EntityTransmuterFactory;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.World;
import games.rednblack.editor.renderer.box2dLight.RayHandler;
import games.rednblack.editor.renderer.components.BoundingBoxComponent;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.particle.ParticleComponent;
import games.rednblack.editor.renderer.data.ProjectInfoVO;
import games.rednblack.editor.renderer.resources.IResourceRetriever;

public class ParticleEffectComponentFactoryV2 extends ComponentFactoryV2 {
    protected ComponentMapper<ParticleComponent> particleCM;

    private final EntityTransmuter transmuter;

    public ParticleEffectComponentFactoryV2(com.artemis.World engine, RayHandler rayHandler, World world, IResourceRetriever rm) {
        super(engine, rayHandler, world, rm);
        transmuter = new EntityTransmuterFactory(engine)
                .add(ParticleComponent.class)
                .remove(BoundingBoxComponent.class)
                .build();
    }

    @Override
    protected void initializeDimensionsComponent(int entity) {
        DimensionsComponent component = dimensionsCM.get(entity);

        ProjectInfoVO projectInfoVO = rm.getProjectVO();
        float boundBoxSize = 70f;
        if (component.boundBox == null)
            component.boundBox = new Rectangle((-boundBoxSize / 2f) / projectInfoVO.pixelToWorld, (-boundBoxSize / 2f) / projectInfoVO.pixelToWorld, boundBoxSize / projectInfoVO.pixelToWorld, boundBoxSize / projectInfoVO.pixelToWorld);
        component.width = boundBoxSize / projectInfoVO.pixelToWorld;
        component.height = boundBoxSize / projectInfoVO.pixelToWorld;
    }

    @Override
    protected void initializeTransientComponents(int entity) {
        super.initializeTransientComponents(entity);

        ParticleComponent component = particleCM.get(entity);

        ParticleEffect particleEffect = new ParticleEffect(rm.getParticleEffect(component.particleName));
        particleEffect.start();
        component.particleEffect = particleEffect;
        ProjectInfoVO projectInfoVO = rm.getProjectVO();
        component.worldMultiplier = 1f / projectInfoVO.pixelToWorld;
        component.scaleEffect(1f);
    }

    @Override
    public void transmuteEntity(int entity) {
        transmuter.transmute(entity);
    }

    @Override
    public int getEntityType() {
        return EntityFactoryV2.PARTICLE_TYPE;
    }

    @Override
    public void setInitialData(int entity, Object data) {
        ParticleComponent component = particleCM.get(entity);
        component.particleName = (String) data;
    }
}