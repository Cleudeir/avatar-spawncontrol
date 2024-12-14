# Avatar-spawncontrol          
## project structure
```                    
avatar-spawncontrol/
    README.md
    mine.xlsx
    build.gradle
    LICENSE.txt
    gradlew
    changelog.txt
    settings.gradle
    CREDITS.txt
    gradle.properties
    gradlew.bat
    src/
        main/
            java/
                com/
                    avatar/
                        avatar_spawncontrol/
                            GlobalConfig.java
                            Main.java
                            server/
                                Events.java
            resources/
                pack.mcmeta
                META-INF/
                    mods.toml
    gradle/
        wrapper/
            gradle-wrapper.jar
            gradle-wrapper.properties                
```
## Projeto: Controlador de Spawn de Mobs para Minecraft Forge

**Objetivo:** Este projeto é um mod para Minecraft Forge que permite aos administradores de servidores controlar com precisão os mecanismos de spawn de mobs.  O mod oferece configurações personalizáveis para distância de spawn, quantidade máxima de mobs por jogador, frequência de despawn, e listas de inclusão/exclusão de tipos de mobs.

**Dependências:** Minecraft Forge.

**Instalação:**  Clone o repositório, instale as dependências do Forge (se necessário), e coloque o arquivo `.jar` do mod na pasta `mods` do seu Minecraft.

**Uso:** O mod é configurado através de um arquivo de configuração.  Os administradores podem ajustar parâmetros para controlar o spawn de mobs. Um comando `/countmonster` mostra a quantidade de monstros próximos ao jogador.

**Arquitetura:** O mod utiliza a API de eventos do Forge.  Eventos de spawn e tick do servidor são interceptados para modificar o comportamento padrão de spawn e despawn de mobs.  Configurações são carregadas de um arquivo e armazenadas em memória.

**Pipeline:** O mod carrega as configurações, monitora eventos do servidor (spawn e tick), aplica as regras de spawn e despawn com base nas configurações, e fornece um comando para o jogador verificar a contagem de monstros próximos.
                
                