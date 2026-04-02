package com.infrastructuresickos.stealth;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Stealth.MOD_ID)
public class Stealth {
    public static final String MOD_ID = "stealth";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public Stealth() {
        MinecraftForge.EVENT_BUS.register(new StealthEventHandler());
        LOGGER.info("Stealth initialized");
    }
}
