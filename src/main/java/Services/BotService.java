package Services;

import Enums.*;
import Models.*;

import java.util.*;
import java.util.stream.*;
import java.util.concurrent.atomic.AtomicInteger;

public class BotService {
    private GameObject bot;
    private PlayerAction playerAction;
    private GameState gameState;
    private boolean afterburner = false;
    private GameObject target = null;
    private static final GameObject worldCenter = new GameObject(UUID.randomUUID(), 0, 0, 0, new Position(), null);
    private boolean teleportFlag = false;
    // private static int teleportHeading = 0;
    private UUID teleporterID;
    // private List<UUID> salvoIDs = new ArrayList<>();
    // private boolean shot = false;
    // private int prevSize = -1;
    // private Position prevPos;
    // private int stuckMeter = 0;
    private GameObject teleTarget = new GameObject(UUID.randomUUID(), 0, 0, 0, new Position(), null);
    private boolean targetSet = false;
    private boolean burner = false;
    private boolean phase = false;
    private boolean teleID = false;
    private boolean escapePhase = false;

    public BotService() {
        this.playerAction = new PlayerAction();
        this.gameState = new GameState();
    }


    public GameObject getBot() {
        return this.bot;
    }

    public void setBot(GameObject bot) {
        this.bot = bot;
    }

    public PlayerAction getPlayerAction() {
        return this.playerAction;
    }

    public void setPlayerAction(PlayerAction playerAction) {
        this.playerAction = playerAction;
    }

    public void computeNextPlayerAction(PlayerAction playerAction) {
        //Scan for teleporter ID
        if(teleportFlag && !teleID){
            scanForTeleporter();
        }

        playerAction.action = PlayerActions.FORWARD;
        int protocol = checkProtocol();
        System.out.println("CURRENT PROTOCOL: " + protocol);
        if(protocol == 1){
            var prey = gameState.getPlayerGameObjects()
                .stream().filter(enemy -> enemy.id != bot.id && getDistanceBetween(enemy, bot) - enemy.getSize() - bot.getSize() <= bot.getSize()*3 && enemy.getSize() <= bot.getSize())
                .sorted(Comparator.comparing(enemy -> getDistanceBetween(bot, enemy))).collect(Collectors.toList());
            
                // Tembak torpedoes
            if(!prey.isEmpty() && bot.getSize() > 30 && bot.getTorpedoCount() > 0){
                target = prey.get(0);
                System.out.println("Torpedoes Shot!");
                playerAction.action = PlayerActions.FIRETORPEDOES;
                //shot = true;
            }
        }else if(protocol == 2){
            // Jika terdapat ancaman player di dekat bot, maka berarah kebalikan dari ancaman tersebut
            var predator = gameState.getPlayerGameObjects()
                        .stream().filter(enemy -> enemy.id != bot.id && getDistanceBetween(enemy, bot) - enemy.getSize() - bot.getSize() <= bot.getSize() + 20 && enemy.getSize() >= bot.getSize())
                        .sorted(Comparator.comparing(enemy -> getDistanceBetween(bot, enemy))).collect(Collectors.toList());
            if(!predator.isEmpty()){
                target = predator.get(0);
                if(bot.TeleporterCount > 0 && bot.getSize() > 50){
                    escape();
                }else{
                    System.out.println("RUNAWAY BITCH");
                    if(!burner && bot.getSize() > 20){
                        playerAction.action = PlayerActions.STARTAFTERBURNER;
                        burner = true;
                        phase = true;
                        escapePhase = true;
                    }else{
                        playerAction.action = PlayerActions.STOPAFTERBURNER;

                    }
                }     
            }
            
        }else{
            if (!gameState.getGameObjects().isEmpty()) {
                if (target == null) {
                    playerAction.heading = findTarget();
                } else {
                    // Mencari target
                    var newTarget = gameState.getGameObjects()
                            .stream().filter(item -> item.id == target.id)
                            .findFirst().or(() -> gameState.getPlayerGameObjects()
                                    .stream().filter(item -> item.id == target.id)
                                    .findFirst());
                    newTarget.ifPresentOrElse(target -> {
                        this.target = target;
                        System.out.println("Found new target: " + target.getGameObjectType());
                        if (target.getSize() < bot.getSize()) {
                            playerAction.heading = getHeadingBetween(target);
                        } else {
                            playerAction.heading = findTarget();
                            
                        }
                    }, () -> playerAction.heading = findTarget());
                    
                }
            }
        }



        // Mengidentifikasi ID teleporter yang sudah di tembak, jika belum diidentifikasi
        // if(teleportFlag){
        //     if(teleporterID == null){
        //         System.out.println("Currently searching for torpedo ID");
        //         var teleporter = gameState.getGameObjects()
        //             .stream().filter(item -> item.getGameObjectType() == ObjectTypes.TELEPORTER)
        //             .min(Comparator.comparing(item -> getDistanceBetween(bot, item)));
                
        //         teleporter.ifPresentOrElse(teleporterShot -> {
        //             teleporterID = teleporterShot.id;
        //             System.out.println("Get TELEPORTER ID: "+teleporterID);
        //         }, ()->{
        //             System.out.println("I CANT FIND THE TELEPORTER");
        //         });
        //     }
        // }   

        // Memastikan tidak keluar border
        if (getGameState().getWorld().getRadius() != null && getDistanceBetween(bot, worldCenter) >= getGameState().getWorld().getRadius() - (2 * bot.getSize())) {
            System.out.println("Oh shit it's the border");
            playerAction.heading = getHeadingBetween(worldCenter);
            target = worldCenter;
        }
        
        //System.out.println("CURRENT SIZE: "+bot.getSize());       

        // Menentukan apakah bot sedang terjebak atau tidak
        // if(gameState.world.getRadius() != null && bot.getSize() == prevSize && bot.getPosition() == prevPos){
        //     stuckMeter++;
        // }else{
        //     stuckMeter = 0;
        // }

        // Jika bot terjebak di tempat yang sama terlalu lama
        // if(stuckMeter >= 10 && !burner){

        //     System.out.println("START AFTERBURNER STUCK");
        //     playerAction.action = PlayerActions.STARTAFTERBURNER;
        //     burner = true;
        // }else if(stuckMeter >= 10 && burner){
        //     System.out.println("STOP AFTERBURNER UNSTUCK");
        //     playerAction.action = PlayerActions.STOPAFTERBURNER;
        //     stuckMeter = 0;
        //     burner = false;
        // }else if(burner){
        //     System.out.println("STOP AFTERBURNER");
        //     playerAction.action = PlayerActions.STOPAFTERBURNER;
        //     burner = false;
        // }

        if(protocol == 1){
            playerAction.heading = getHeadingBetween(target);
        }else if(protocol == 2){
            if(getHeadingBetween(target) >= 180){
                playerAction.heading = getHeadingBetween(target) - 180;
            }else{
                playerAction.heading = getHeadingBetween(target) + 180;
            }
        }

        // prevSize = bot.getSize();
        // prevPos = bot.getPosition();
        phase = false;

        this.playerAction = playerAction;
    }

    private int checkProtocol(){
        var predator = gameState.getPlayerGameObjects()
                        .stream().filter(enemy -> enemy.id != bot.id && getDistanceBetween(enemy, bot) - enemy.getSize() - bot.getSize() <= bot.getSize()*2 && enemy.getSize() >= bot.getSize())
                        .sorted(Comparator.comparing(enemy -> getDistanceBetween(bot, enemy))).collect(Collectors.toList());

        var prey = gameState.getPlayerGameObjects()
            .stream().filter(enemy -> enemy.id != bot.id && getDistanceBetween(enemy, bot) - enemy.getSize() - bot.getSize() <= bot.getSize()*3 && enemy.getSize() <= 2*bot.getSize())
            .sorted(Comparator.comparing(enemy -> getDistanceBetween(bot, enemy))).collect(Collectors.toList());
        
        if(!predator.isEmpty()){
            return 2;
        }else if(!prey.isEmpty()){
            return 1;
        }else{
            return 0;
        }
    }

    private void scanForTeleporter(){
        if(teleportFlag && teleporterID == null && !teleID){
            var teleporter = gameState.getGameObjects()
                .stream().filter(item -> item.getGameObjectType() == ObjectTypes.TELEPORTER)
                .min(Comparator.comparing(item -> getDistanceBetween(bot, item)));

            teleporter.ifPresent(tele -> {
                teleporterID = tele.getId();
                teleID = true;
            });
        }
    }

    private void escape(){
        var danger = gameState.getPlayerGameObjects()
                .stream().filter(enemy -> enemy.id != bot.id && getDistanceBetween(enemy, bot) - enemy.getSize() - bot.getSize() <= bot.getSize() + 20 && enemy.getSize() >= bot.getSize())
                .min(Comparator.comparing(enemy -> getDistanceBetween(bot, enemy)));
        
        var prey = gameState.getPlayerGameObjects()
            .stream().filter(enemy -> enemy.id != bot.id && enemy.getSize() <= bot.getSize())
            .sorted(Comparator.comparing(enemy -> getDistanceBetween(bot, enemy))).collect(Collectors.toList());
        
        var remaining = gameState.getPlayerGameObjects()
            .stream().filter(item -> item.id != bot.getId())
            .sorted(Comparator.comparing(item -> getDistanceBetween(bot, item))).collect(Collectors.toList());
        
        
        
        if(remaining.size() > 1 && !teleportFlag && bot.getSize() - 20 >= danger.get().getSize()/2){
            danger.ifPresent(enemy -> {
                System.out.println("ENEMY ALERT!");
                System.out.println("CURRENT SIZE: " + bot.getSize());
                System.out.println("ENEMY SIZE: "+ enemy.getSize());
                System.out.println("Salvo ammount: " + bot.getTorpedoCount());
                
                if(!prey.isEmpty()){
                    playerAction.heading = getHeadingBetween(prey.get(0));
                    teleTarget.setPosition(prey.get(0).getPosition());
                    targetSet = true;
                }else if(gameState.world.getRadius() != null && (gameState.world.getRadius() - bot.getPosition().getX() <= bot.getSize()*2 || gameState.world.getRadius() - bot.getPosition().getY() <= bot.getSize()*2)){
                    System.out.println("SENDING TELEPORTER TO CENTER!");
                    playerAction.heading = getHeadingBetween(worldCenter);
                }else{
                    System.out.println("SENDING TELEPORTER AWAY FROM ENEMY");
                    if(getHeadingBetween(enemy) >= 180){
                        playerAction.heading = getHeadingBetween(enemy) - 180;
                    }else{
                        playerAction.heading = getHeadingBetween(enemy) + 180;
                    }
                }

                playerAction.action = PlayerActions.FIRETELEPORT;
                System.out.println("TELEPORTER DEPLOYED");
                teleportFlag = true;    
            });
        }else if (teleportFlag){
            if(targetSet){
                var teleporter = gameState.getGameObjects()
                    .stream().filter(item -> item.getGameObjectType() == ObjectTypes.TELEPORTER && getDistanceBetween(item, teleTarget) <= bot.getSize())
                    .min(Comparator.comparing(item -> getDistanceBetween(bot, item)));
                
                teleporter.ifPresent(tele -> {
                    System.out.println("TIME TO TELEPORT!");
                    playerAction.action = PlayerActions.TELEPORT;
                    teleportFlag = false;
                    targetSet = false;
                });
            }else{
                var teleporter = gameState.getGameObjects()
                .stream().filter(item -> item.getGameObjectType() == ObjectTypes.TELEPORTER && item.getId() == teleporterID)
                .min(Comparator.comparing(item -> getDistanceBetween(bot, item)));
            
                teleporter.ifPresent(teleporterShot -> {
                var dangerT = gameState.getPlayerGameObjects()
                .stream().filter(enemy -> enemy.id != bot.id && getDistanceBetween(enemy, teleporterShot) - enemy.getSize() - bot.getSize() <=  bot.getSize()*2)
                .min(Comparator.comparing(item -> getDistanceBetween(teleporterShot, item)));

                dangerT.ifPresentOrElse(item -> {
                    System.out.println("Not teleport worthy yet");
                }, ()->{
                    System.out.println("TIME TO TELEPORT!");
                    playerAction.action = PlayerActions.TELEPORT;
                    teleportFlag = false;
                    teleporterID = null;
                    teleID = false;
                });
            });
            } 
        }
        
        
                
    }

    private int findTarget() {
        AtomicInteger heading = new AtomicInteger();
        var nearestPlayer = gameState.getPlayerGameObjects()
                .stream().filter(item -> item.id != bot.id)
                .min(Comparator.comparing(item -> getDistanceBetween(bot, item)));
        var nearestFood = gameState.getGameObjects()
                .stream().filter(item -> item.getGameObjectType() == ObjectTypes.FOOD)
                .min(Comparator.comparing(item -> getDistanceBetween(bot, item)));
        var nearestGasCloud = gameState.getGameObjects()
                .stream().filter(item -> item.getGameObjectType() == ObjectTypes.GASCLOUD)
                .min(Comparator.comparing(item -> getDistanceBetween(bot, item)));

        nearestPlayer.ifPresent(enemy -> nearestFood.ifPresent(food -> {
            if (enemy.getSize() < bot.getSize()) {
                if (afterburner) {
                    toggleAfterburner();
                }
                heading.set(getHeadingBetween(enemy));
                target = enemy;
            } else if (enemy.getSize() > bot.getSize()) {
                if (afterburner) {
                    toggleAfterburner();
                }
                if (getDistanceBetween(bot, enemy) + enemy.getSize() < (2 * bot.getSize())) {
                    heading.set(getHeadingBetween(enemy)%360);
                } else {
                    heading.set(getHeadingBetween(food));
                    target = food;
                }
            } else if (nearestGasCloud.isPresent()) {
                var gasCloud = nearestGasCloud.get();
                if (getDistanceBetween(bot, gasCloud) - gasCloud.getSize() < (2 * bot.getSize())) {
                    heading.set(-getHeadingBetween(gasCloud));
                    if (!afterburner && bot.getSize() > 20) {
                        toggleAfterburner();
                    }
                }
            } else {
                if (afterburner) {
                    toggleAfterburner();
                }
                heading.set(getHeadingBetween(food));
                target = food;
            }
        }));

        return heading.get();
    }

    private void toggleAfterburner() {
    //    afterburner = !afterburner;
    //    playerAction.action = afterburner ? PlayerActions.STARTAFTERBURNER : PlayerActions.STOPAFTERBURNER;
    }

    public GameState getGameState() {
        return this.gameState;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
        updateSelfState();
    }

    private void updateSelfState() {
        Optional<GameObject> optionalBot = gameState.getPlayerGameObjects().stream().filter(gameObject -> gameObject.id.equals(bot.id)).findAny();
        optionalBot.ifPresent(bot -> this.bot = bot);
    }

    private double getDistanceBetween(GameObject object1, GameObject object2) {
        var triangleX = Math.abs(object1.getPosition().x - object2.getPosition().x);
        var triangleY = Math.abs(object1.getPosition().y - object2.getPosition().y);
        return Math.sqrt(triangleX * triangleX + triangleY * triangleY);
    }

    private int getHeadingBetween(GameObject otherObject) {
        var direction = toDegrees(Math.atan2(otherObject.getPosition().y - bot.getPosition().y,
                otherObject.getPosition().x - bot.getPosition().x));
        return (direction + 360) % 360;
    }

    private int getHeadingBetweenCenter(GameObject otherObject) {
        var direction = toDegrees(Math.atan2(otherObject.getPosition().y - worldCenter.getPosition().y,
                otherObject.getPosition().x - worldCenter.getPosition().x));
        return (direction + 360) % 360;
    }

    private int toDegrees(double v) {
        return (int) (v * (180 / Math.PI));
    }
}
