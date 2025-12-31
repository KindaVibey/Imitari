/**
 * Imitari API - Create blocks that copy and mimic other blocks.
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * // 1. Create your block
 * public class MyBlock extends CopyBlockBase {
 *     public MyBlock(Properties props) {
 *         super(props, 0.5f); // 0.5 = half mass
 *     }
 * }
 *
 * // 2. Register it
 * public static final RegistryObject<Block> MY_BLOCK = BLOCKS.register("my_block",
 *     () -> new MyBlock(BlockBehaviour.Properties.of()
 *         .strength(0.5F)
 *         .sound(SoundType.WOOD)
 *         .noOcclusion()));
 *
 * // 3. Enable features in your mod setup
 * public void commonSetup(FMLCommonSetupEvent event) {
 *     event.enqueueWork(() -> {
 *         CopyBlockRegistration.registerForMod("yourmodid");
 *     });
 * }
 * }</pre>
 *
 * <h2>Package Structure</h2>
 * <ul>
 *   <li>{@link com.vibey.imitari.api} - Core API classes</li>
 *   <li>{@link com.vibey.imitari.api.blockentity} - Block entity interfaces</li>
 *   <li>{@link com.vibey.imitari.api.registration} - Registration helpers</li>
 *   <li>{@link com.vibey.imitari.api.tags} - Dynamic tag system</li>
 *   <li>{@link com.vibey.imitari.api.vs2} - Valkyrien Skies 2 integration</li>
 *   <li>{@link com.vibey.imitari.block.base} - Convenient base classes</li>
 * </ul>
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link com.vibey.imitari.api.ICopyBlock} - Main interface for CopyBlocks</li>
 *   <li>{@link com.vibey.imitari.api.CopyBlockAPI} - Static utility methods</li>
 *   <li>{@link com.vibey.imitari.block.base.CopyBlockBase} - Convenient base class</li>
 *   <li>{@link com.vibey.imitari.api.registration.CopyBlockRegistration} - Registration helper</li>
 * </ul>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li><b>Dynamic Textures:</b> Automatically copy textures from any block</li>
 *   <li><b>Dynamic Physics:</b> Inherit hardness, blast resistance from copied blocks</li>
 *   <li><b>Dynamic Tags:</b> Automatically inherit all tags from copied blocks</li>
 *   <li><b>VS2 Integration:</b> Automatic ship mass calculation (if VS2 installed)</li>
 *   <li><b>Easy to Use:</b> Extend one class, set mass multiplier, done!</li>
 * </ul>
 *
 * <h2>Mass Multipliers</h2>
 * <p>Common values:</p>
 * <ul>
 *   <li>1.0f - Full block</li>
 *   <li>0.75f - Stairs</li>
 *   <li>0.5f - Slab</li>
 *   <li>0.25f - Quarter block</li>
 *   <li>0.125f - 1/8 layer</li>
 * </ul>
 *
 * <h2>Compatibility</h2>
 * <ul>
 *   <li>Minecraft 1.20.1</li>
 *   <li>Forge 47.x</li>
 *   <li>Optional: Valkyrien Skies 2</li>
 * </ul>
 *
 * @see com.vibey.imitari.api.ICopyBlock
 * @see com.vibey.imitari.api.CopyBlockAPI
 * @see com.vibey.imitari.block.base.CopyBlockBase
 *
 * @author KindaVibey
 * @version 0.1.0
 */
package com.vibey.imitari.api;