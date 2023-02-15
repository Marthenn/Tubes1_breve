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
    private static int teleportHeading = 0;
    private UUID teleporterID;

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
//        playerAction.action = PlayerActions.FORWARD;
//        playerAction.heading = new Random().nextInt(360);
//
//        if (!gameState.getGameObjects().isEmpty()) {
//            var foodList = gameState.getGameObjects()
//                    .stream().filter(item -> item.getGameObjectType() == ObjectTypes.FOOD)
//                    .sorted(Comparator
//                            .comparing(item -> getDistanceBetween(bot, item)))
//                    .collect(Collectors.toList());
//
//            playerAction.heading = getHeadingBetween(foodList.get(0));
//        }
//
//        this.playerAction = playerAction;
        playerAction.action = PlayerActions.FORWARD;

        

        if (!gameState.getGameObjects().isEmpty()) {
            if (target == null) {
                playerAction.heading = findTarget();
            } else {
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

                var nearTorpedoes = gameState.getGameObjects()
                        .stream().filter(item -> item.getGameObjectType() == ObjectTypes.TORPEDOSALVO)
                        .sorted(Comparator.comparing(item -> getDistanceBetween(bot, item))).collect(Collectors.toList());
                
                if(nearTorpedoes.size() > 0 && getDistanceBetween(bot, nearTorpedoes.get(0)) < 60){
                    System.out.println("TORPEDO ALERT!");
                    System.out.println("Distance: " + getDistanceBetween(bot, nearTorpedoes.get(0)));
                    System.out.println("Current size: "+bot.getSize());
                    System.out.println("Shield amount: " + bot.getShieldCount());
                }

                // if(runaway){
                //     playerAction.action = PlayerActions.FIRETELEPORT;
                //     teleportFlag = true;
                //     teleportHeading = playerAction.heading;
                // }else 
                if(nearTorpedoes.size() > 0 &&  (getDistanceBetween(bot, nearTorpedoes.get(0)) <= bot.getSize() + 60) && bot.getSize() >= 35 && (nearTorpedoes.get(0).currentHeading > 30 + bot.currentHeading || nearTorpedoes.get(0).currentHeading < bot.currentHeading - 30) && bot.getShieldCount() > 0){
                        System.out.println("Shield Activated!");
                        playerAction.action = PlayerActions.ACTIVATESHIELD;
                }else if(target.gameObjectType==ObjectTypes.PLAYER && target.gameObjectType!=ObjectTypes.FOOD && bot.getSize() > 30 && getDistanceBetween(bot, target) - target.getSize() - bot.getSize() < bot.getSize()*2 && bot.getTorpedoCount() > 0){
                    System.out.println("Torpedoes Shot!");
                    playerAction.action = PlayerActions.FIRETORPEDOES;
                }

                if(bot.getSize() > 30){
                    getSupernovaDrop();
                }
                if(bot.getSupernovaCount()>0){
                    System.out.println("SUPERNOVA ACQUIRED BITCHEEEESSSS");
                    initiateSupernova();
                }

                
            }
        }


        if (getGameState().getWorld().getRadius() != null && getDistanceBetween(bot, worldCenter) >= getGameState().getWorld().getRadius() - (2 * bot.getSize())) {
            playerAction.heading = getHeadingBetween(worldCenter);
            target = worldCenter;
        }

        // if(teleportFlag){
        //     var teleporterShot = gameState.getGameObjects()
        //         .stream().filter(item -> (item.getGameObjectType() == ObjectTypes.TELEPORTER) && (item.currentHeading == teleportHeading))
        //         .sorted(Comparator.comparing(item -> getDistanceBetween(bot, item))).collect(Collectors.toList());
            
        //     var playerListTeleporter = gameState.getPlayerGameObjects()
        //         .stream().filter(item -> item.id != this.bot.id)
        //         .sorted(Comparator
        //                 .comparing(item -> getDistanceBetween(teleporterShot.get(0), item)))
        //         .collect(Collectors.toList());
        //     // var gasClouds = gameState.getGameObjects()
        //     //     .stream().filter(item -> (item.getGameObjectType() == ObjectTypes.GASCLOUD) && (getDistanceBetween(teleporterShot.get(0), item) - item.getSize() > 0))
        //     //     .sorted(Comparator.comparing(item -> getDistanceBetween(teleporterShot.get(0), item))).collect(Collectors.toList());
            

        //     // if(gasClouds.size() == 0){
        //     //     System.out.println("Teleporter shot!");
        //     //     playerAction.action = PlayerActions.TELEPORT;
        //     //     teleportFlag = false;
        //     //     teleportHeading = 0;
        //     // }

        //     if(getDistanceBetween(teleporterShot.get(0), playerListTeleporter.get(0)) > 100){
        //         System.out.println("Teleporter shot!");
        //         playerAction.action = PlayerActions.TELEPORT;
        //         teleportFlag = false;
        //         teleportHeading = 0;
        //     }
        // }
        searchForTeleporter();

        this.playerAction = playerAction;
    }

    private void searchForTeleporter(){
        var teleporter = gameState.getGameObjects()
                .stream().filter(item -> item.getGameObjectType() == ObjectTypes.TELEPORTER && item.id == teleporterID)
                .min(Comparator.comparing(item -> getDistanceBetween(bot, item)));
        
        teleporter.ifPresentOrElse(null, ()-> teleportFlag = false);
    }

    // private void escape(){
    //     var danger = gameState.getPlayerGameObjects()
    //             .stream().filter(enemy -> enemy.id == bot.id && getDistanceBetween(enemy, bot) - enemy.getSize() - bot.getSize() <= (bot.getSize()*2) && enemy.getSize() >= bot.getSize())
    //             .min(Comparator.comparing(item -> getDistanceBetween(bot, item)));
        
    //     if(!teleportFlag){
    //         danger.ifPresent(enemy -> {
    //             if(getHeadingBetween(enemy) >= 180){
    //                 playerAction.heading = getHeadingBetween(enemy) - 180;
    //             }else{
    //                 playerAction.heading = getHeadingBetween(enemy) + 180;
    //             }
    //             playerAction.action = PlayerActions.TELEPORT;
    //             teleportFlag = true;
    //         });
    //     }else{

    //     }
                
    // }
    
    private void initiateSupernova(){
        var players = gameState.getPlayerGameObjects()
                        .stream().filter(item -> item.id != bot.id)
                        .sorted(Comparator.comparing(item -> getDistanceBetween(bot, item))).collect(Collectors.toList());
        double dist12 = 0;double dist23 = 0; double dist13 = 0;
        if(players.size() > 1){
            dist12 = getDistanceBetween(players.get(0), players.get(1));
            if(players.size() > 2){
                dist13 = getDistanceBetween(players.get(0), players.get(2));
                dist23 = getDistanceBetween(players.get(1), players.get(2));
            }
        }

        if(dist12 <=100 && dist13 <=100 && dist23<=100){
            playerAction.heading = getHeadingBetween(players.get(0));
            playerAction.action = PlayerActions.FIRESUPERNOVA;
        }
    }

    private void getSupernovaDrop(){
        var pickup = gameState.getGameObjects()
                .stream().filter(item -> item.getGameObjectType() == ObjectTypes.TELEPORTER && item.id == teleporterID)
                .min(Comparator.comparing(item -> getDistanceBetween(bot, item)));
        if(!teleportFlag){
            pickup.ifPresent(pickNova -> {
                if(bot.getTeleporterCount() > 0){
                    System.out.println("SUPERNOVA DETECTED");
                    playerAction.heading = getHeadingBetween(pickNova);
                    playerAction.action = PlayerActions.FIRETELEPORT;
                    teleportFlag = true;
                }
            });
        }else{
            var teleporter = gameState.getGameObjects()
                .stream().filter(item -> item.getGameObjectType() == ObjectTypes.TELEPORTER && item.id == teleporterID)
                .min(Comparator.comparing(item -> getDistanceBetween(bot, item)));
            
            teleporter.ifPresent(teleportShot -> pickup.ifPresent(pickNova ->{
                var players = gameState.getPlayerGameObjects()
                    .stream().filter(item -> item.id != bot.id && (getDistanceBetween(teleportShot, item) - item.getSize() <= bot.getSize()*2 && item.getSize() >= bot.getSize()))
                    .sorted(Comparator.comparing(item -> getDistanceBetween(teleportShot, item))).collect(Collectors.toList());
                
                if(players.isEmpty() && getDistanceBetween(pickNova, teleportShot) <= bot.getSize()){
                    playerAction.action = PlayerActions.TELEPORT;
                    teleportFlag = false;
                }
            }));
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
                System.out.println("ENEMY ALERT!");
                System.out.println("CURRENT SIZE: " + bot.getSize());
                System.out.println("Salvo ammount: " + bot.getTorpedoCount());
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
                if (getDistanceBetween(bot, gasCloud) + gasCloud.getSize() < (2 * bot.getSize())) {
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
//        afterburner = !afterburner;
//        playerAction.action = afterburner ? PlayerActions.STARTAFTERBURNER : PlayerActions.STOPAFTERBURNER;
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

    private int toDegrees(double v) {
        return (int) (v * (180 / Math.PI));
    }
}
