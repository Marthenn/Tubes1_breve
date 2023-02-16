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
    private List<UUID> salvoIDs = new ArrayList<>();
    private boolean shot = false;
    private int prevSize = -1;
    private Position prevPos;
    private int stuckMeter = 0;
    private boolean burner = false;

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
        playerAction.action = PlayerActions.FORWARD;

        

        if (!gameState.getGameObjects().isEmpty()) {
            if (target == null) {
                playerAction.heading = findTarget();
            } else {
                //Identify torpedoes
                if(shot){
                    var salvos = gameState.getGameObjects()
                        .stream().filter(item -> item.getGameObjectType() == ObjectTypes.TORPEDOSALVO)
                        .min(Comparator.comparing(item -> getDistanceBetween(bot, item)));
                    
                    salvos.ifPresent(salvo -> {
                        salvoIDs.add(salvo.getId());
                    });

                    shot = false;
                }


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
                
                
                for(int i = 0; i < nearTorpedoes.size(); i++){
                    for(int j = 0; j < salvoIDs.size(); j++){
                        if(nearTorpedoes.get(i).getId() == salvoIDs.get(j)){
                           nearTorpedoes.remove(i);
                           i--;
                           break;
                        }
                    }
                }

                var nearDanger = gameState.getPlayerGameObjects()
                        .stream().filter(item -> item.id != bot.getId() && getDistanceBetween(item, bot) - item.getSize() - bot.getSize() <= bot.getSize() && item.getSize() - bot.getSize() <= 10)
                        .sorted(Comparator.comparing(item -> getDistanceBetween(bot, item))).collect(Collectors.toList());

                if(nearTorpedoes.size() > 0 && getDistanceBetween(bot, nearTorpedoes.get(0)) < 60){
                    System.out.println("TORPEDO ALERT!");
                    System.out.println("Distance: " + getDistanceBetween(bot, nearTorpedoes.get(0)));
                    System.out.println("Current size: "+bot.getSize());
                    System.out.println("Shield amount: " + bot.getShieldCount());
                }

                if(nearTorpedoes.size() > 0 &&  (getDistanceBetween(bot, nearTorpedoes.get(0)) <= bot.getSize() + 60) && bot.getSize() >= 35 && (nearTorpedoes.get(0).currentHeading > 30 + bot.currentHeading || nearTorpedoes.get(0).currentHeading < bot.currentHeading - 30) && bot.getShieldCount() > 0){
                        System.out.println("Shield Activated!");
                        playerAction.action = PlayerActions.ACTIVATESHIELD;
                }else if((target.gameObjectType==ObjectTypes.PLAYER || !nearDanger.isEmpty()) && target.gameObjectType!=ObjectTypes.FOOD && bot.getSize() > 30 && getDistanceBetween(bot, target) - target.getSize() - bot.getSize() < bot.getSize()*2 && bot.getTorpedoCount() > 0){
                    System.out.println("Torpedoes Shot!");
                    if(target.gameObjectType != ObjectTypes.PLAYER){
                        playerAction.heading = getHeadingBetween(nearDanger.get(0));
                        
                    }
                    playerAction.action = PlayerActions.FIRETORPEDOES;
                    shot = true;
                }

                if(teleportFlag){
                    if(teleporterID == null){
                        System.out.println("Currently searching for torpedo ID");
                        var teleporter = gameState.getGameObjects()
                            .stream().filter(item -> item.getGameObjectType() == ObjectTypes.TELEPORTER)
                            .min(Comparator.comparing(item -> getDistanceBetween(bot, item)));
                        
                        teleporter.ifPresentOrElse(teleporterShot -> {
                            teleporterID = teleporterShot.id;
                            System.out.println("Get TELEPORTER ID: "+teleporterID);
                        }, ()->{
                            System.out.println("I CANT FIND THE TELEPORTER");
                        });
                    }
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
        System.out.println("CURRENT SIZE: "+bot.getSize());
        escape();



        if(bot.getSize() == prevSize && bot.getPosition() == prevPos){
            stuckMeter++;
        }

        if(stuckMeter >= 10 && !burner){
            System.out.println("START AFTERBURNER");
            playerAction.action = PlayerActions.STARTAFTERBURNER;
            burner = true;
        }else if(stuckMeter >= 10 && burner){
            System.out.println("STOP AFTERBURNER");
            playerAction.action = PlayerActions.STOPAFTERBURNER;
            stuckMeter = 0;
            burner = false;
        }

        prevSize = bot.getSize();
        prevPos = bot.getPosition();

        this.playerAction = playerAction;
    }

    private void escape(){

        var danger = gameState.getPlayerGameObjects()
                .stream().filter(enemy -> enemy.id != bot.id && getDistanceBetween(enemy, bot) - enemy.getSize() - bot.getSize() <= (bot.getSize()*2) + 20 && enemy.getSize() >= bot.getSize())
                .min(Comparator.comparing(item -> getDistanceBetween(bot, item)));
        
        var remaining = gameState.getPlayerGameObjects()
            .stream().filter(item -> item.id != bot.getId())
            .sorted(Comparator.comparing(item -> getDistanceBetween(bot, item))).collect(Collectors.toList());
        

        if(remaining.size() > 1 && !teleportFlag && bot.TeleporterCount> 0 && bot.getSize()> 50){
            danger.ifPresent(enemy -> {
                System.out.println("ENEMY ALERT!");
                System.out.println("CURRENT SIZE: " + bot.getSize());
                System.out.println("ENEMY SIZE: "+ enemy.getSize());
                System.out.println("Salvo ammount: " + bot.getTorpedoCount());
                if(gameState.world.getRadius() != null && bot.getPosition().getX() - gameState.world.getRadius() <= bot.getSize()*2 || bot.getPosition().getY() - gameState.world.getRadius() <= bot.getSize()*2){
                    playerAction.heading = getHeadingBetween(worldCenter);
                }else{
                    if(getHeadingBetween(enemy) >= 180){
                        playerAction.heading = getHeadingBetween(enemy) - 180;
                    }else{
                        playerAction.heading = getHeadingBetween(enemy) + 180;
                    }
                }
                if(bot.getSize() - 20 >= enemy.getSize()/2){
                    playerAction.action = PlayerActions.FIRETELEPORT;
                    System.out.println("TELEPORTER DEPLOYED");
                    teleportFlag = true;
                }       
            });
        }else if (teleportFlag && teleporterID!=null){
            var teleporter = gameState.getGameObjects()
                .stream().filter(item -> item.getGameObjectType() == ObjectTypes.TELEPORTER && item.id == teleporterID)
                .min(Comparator.comparing(item -> getDistanceBetween(bot, item)));
            
            teleporter.ifPresent(teleporterShot -> {
                var dangerT = gameState.getPlayerGameObjects()
                .stream().filter(enemy -> enemy.id != bot.id && getDistanceBetween(enemy, teleporterShot) - enemy.getSize() - bot.getSize() <=  bot.getSize() + 60)
                .min(Comparator.comparing(item -> getDistanceBetween(bot, item)));

                dangerT.ifPresentOrElse(item -> {
                    System.out.println("Not teleport worthy yet");
                }, ()->{
                    System.out.println("TIME TO TELEPORT!");
                    playerAction.action = PlayerActions.TELEPORT;
                    teleportFlag = false;
                    teleporterID = null;
                });
            });
        }
        
        
                
    }
    
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


    private int findTarget() {
        AtomicInteger heading = new AtomicInteger();
        var nearestPlayer = gameState.getPlayerGameObjects()
                .stream().filter(item -> item.id != bot.id)
                .min(Comparator.comparing(item -> getDistanceBetween(bot, item)));
        var nearestFood = gameState.getGameObjects()
                .stream().filter(item -> item.getGameObjectType() == ObjectTypes.FOOD && gameState.world.getRadius()*Math.cos(Math.toRadians(getHeadingBetweenCenter(item))) - item.getPosition().getX() >= bot.getSize() && gameState.world.getRadius()*Math.sin(Math.toRadians(getHeadingBetweenCenter(item))) - item.getPosition().getY() >= bot.getSize())
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
       afterburner = !afterburner;
       playerAction.action = afterburner ? PlayerActions.STARTAFTERBURNER : PlayerActions.STOPAFTERBURNER;
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
