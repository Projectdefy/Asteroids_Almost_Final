package enhancedAsteroids;

import java.awt.event.*;
import java.util.Iterator;
import java.util.Random;

import javax.swing.*;

import enhancedAsteroids.Constants;
import enhancedAsteroids.EnhancedController;
import enhancedAsteroids.Display;
import enhancedAsteroids.Participant;
import enhancedAsteroids.ParticipantState;
import enhancedAsteroids.destroyers.AlienShip;
import enhancedAsteroids.participants.Asteroid;
import enhancedAsteroids.participants.Ship;
import enhancedAsteroids.participants.PowerUp;

import static enhancedAsteroids.Constants.END_DELAY;
import static enhancedAsteroids.Constants.*;

/**
 * Controls a game of Asteroids.
 */
public class EnhancedController implements KeyListener, ActionListener {

	// power-up in current use
	private PowerUp pwr;

	// power up Timer if needed.
	private Timer powerUpTimer;

	// The current level
	private int level;

	// Total score
	private int score;

	// The state of all the Participants
	private ParticipantState pstate;

	// The ship (if one is active) or null (otherwise)
	private Ship ship;

	// When this timer goes off, it is time to refresh the animation
	private Timer refreshTimer;

	// The time at which a transition to a new stage of the game should be made.
	// A transition is scheduled a few seconds in the future to give the user
	// time to see what has happened before doing something like going to a new
	// level or resetting the current level.
	private long transitionTime;

	// Number of lives left
	private int lives;

	// Calls the alien ship
	private Timer alienShipTimer;

	// The game display
	private Display display;

	// Will easily help make left rotations with a boolean
	private boolean turnLeft;

	// Will easily help make right rotations with a boolean
	private boolean turnRight;

	// Will easily help to go forward with a boolean
	private boolean goForward;

	// Helps to fire the bullets in a later method
	private boolean fireBullet;

	// This timer will help delay the beat the correct amount between each beat
	private Timer beatTimer;

	// Will go between 1 and 2 to play the beat
	private int alternateBeat;

	// Used to get the return of the getAlienShip
	private AlienShip alienShip;

	// score multiplier
	private int multiplier;

	// score multiplier calculator
	private double calculator;

	// if powered up, how many ms while you remain that way.
	private int secondsLeftPowered;

	// power up effects

	private boolean rapidFire;

	private boolean maxMultiplier;

	private boolean bigGunLoaded;

	private boolean invincible;

	/**
	 * Constructs a controller to coordinate the game and screen
	 */
	public EnhancedController() {

		// Initialize the ParticipantState
		pstate = new ParticipantState();

		// Set up the refresh timer.
		refreshTimer = new Timer(FRAME_INTERVAL, this);

		// sets up the alienShipTimer
		alienShipTimer = new Timer((5 + Constants.RANDOM.nextInt(6)) * 1000, this);

		// sets up powerup timer.
		powerUpTimer = new Timer(5000, this);

		// Clear the transitionTime
		transitionTime = Long.MAX_VALUE;

		// Record the display object
		display = new Display(this);

		// Beat Timer
		beatTimer = new Timer(Constants.INITIAL_BEAT, this);

		// score multiplier
		multiplier = 1;

		// initializes the powerup things.
		powerDown();
		pwr = null;

		// Bring up the splash screen and start the refresh timer
		splashScreen();
		display.setVisible(true);
		refreshTimer.start();
		powerUpTimer.start();
	}

	/**
	 * Returns the ship, or null if there isn't one
	 */
	public Ship getShip() {
		return ship;
	}

	/**
	 * Returns the alienShip, or null if there isn't one
	 */
	public AlienShip getAlienShip() {
		return alienShip;
	}

	/**
	 * Configures the game screen to display the splash screen
	 */
	private void splashScreen() {
		// Clear the screen, reset the level, and display the legend
		clear();
		level = 1;
		display.setLegend("Asteroids");

		// Place four asteroids near the corners of the screen.
		placeAsteroids(level);
	}

	/**
	 * The game is over. Displays a message to that effect.
	 */
	private void finalScreen() {
		display.setLegend(GAME_OVER);
		display.removeKeyListener(this);
	}

	/**
	 * Place a new ship in the center of the screen. Remove any existing ship
	 * first.
	 */
	private void placeShip() {
		// Place a new ship
		Participant.expire(ship);

		turnLeft = false;
		turnRight = false;
		goForward = false;
		fireBullet = false;
		ship = new Ship(SIZE / 2, SIZE / 2, -Math.PI / 2, this);
		addParticipant(ship);
		display.setLegend("");
	}

	/**
	 * Places asteroids based on level near the corners of the screen. Gives
	 * them random velocities and rotations.
	 */
	private void placeAsteroids(int level) {
		addParticipant(new Asteroid(0, 2, EDGE_OFFSET, EDGE_OFFSET, SLOW_ASTEROID_SPEED, this));
		addParticipant(new Asteroid(1, 2, SIZE - EDGE_OFFSET, EDGE_OFFSET, SLOW_ASTEROID_SPEED, this));
		addParticipant(new Asteroid(2, 2, EDGE_OFFSET, SIZE - EDGE_OFFSET, SLOW_ASTEROID_SPEED, this));
		addParticipant(new Asteroid(3, 2, SIZE - EDGE_OFFSET, SIZE - EDGE_OFFSET, SLOW_ASTEROID_SPEED, this));
		for (int i = 1; i < level; i++) {
			addParticipant(new Asteroid(new Random().nextInt(4), 2, new Random().nextInt(5) * EDGE_OFFSET,
					new Random().nextInt(5) * EDGE_OFFSET, SLOW_ASTEROID_SPEED, this));
		}
	}

	/**
	 * Clears the screen so that nothing is displayed
	 */
	private void clear() {
		pstate.clear();
		display.setLegend("");
		if (ship != null) {
			ship.notAccelerating();
			ship = null;
		}
		if (pstate.isAlienShip() == true) {
			Participant.expire(alienShip);
			alienShip = null;
		}
	}

	/**
	 * Sets things up and begins a new game.
	 */
	private void initialScreen() {
		beatTimer.stop();
		beatTimer.setDelay(Constants.INITIAL_BEAT);
		beatTimer.start();

		// Clear the screen
		clear();

		// Place four asteroids
		placeAsteroids(1);

		// Place the ship
		placeShip();

		// Reset statistics
		lives = 3;
		level = 1;
		score = 0;

		// Settings the labels for lives, level, score
		display.setLives(lives);
		display.setLevel(level);
		display.setScore(score);

		// Will help with turning
		turnLeft = false;
		turnRight = false;
		goForward = false;
		fireBullet = false;

		// Starts beat 1
		alternateBeat = 2;

		// Start listening to events (but don't listen twice)
		display.removeKeyListener(this);
		display.addKeyListener(this);

		// Give focus to the game screen
		display.requestFocusInWindow();
	}

	/**
	 * Adds a new Participant
	 */
	public void addParticipant(Participant p) {
		pstate.addParticipant(p);
	}

	/**
	 * The ship has been destroyed
	 */
	public void shipDestroyed() {
		// Null out the ship
		ship = null;

		// Decrement lives
		lives--;

		// Changes the display of lives
		display.setLives(lives);

		// restarts beat
		beatTimer.restart();

		// Since the ship was destroyed, schedule a transition
		scheduleTransition(END_DELAY);
	}

	/**
	 * An asteroid of the given size has been destroyed
	 */
	public void asteroidDestroyed(int size) {

		switch (size) {
		case 0:
			Participant.getSounds().playBangSmallClip();
		case 1:
			Participant.getSounds().playBangMediumClip();
		case 2:
			Participant.getSounds().playBangLargeClip();
		}

		// effects the multiplier calculator.
		calculator += .28;

		// Adds the score
		scoreAdder(Constants.ASTEROID_SCORE[size]);

		// If all the asteroids are gone, schedule a transition
		if (pstate.countAsteroids() == 0) {
			beatTimer.stop();
			display.setLegend("Level " + (1 + level));
			scheduleTransition(END_DELAY);
		}
	}

	/**
	 * Schedules a transition m msecs in the future
	 */
	private void scheduleTransition(int m) {
		transitionTime = System.currentTimeMillis() + m;
	}

	/**
	 * This method will be invoked because of button presses and timer events.
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		// The start button has been pressed. Stop whatever we're doing
		// and bring up the initial screen
		if (e.getSource() instanceof JButton) {
			initialScreen();
		}
		// Will alternate beat
		else if (e.getSource() == beatTimer && alternateBeat == 1) {
			Participant.getSounds().playBeat1Clip();
			beatTimer.setDelay(Math.max(Constants.FASTEST_BEAT, beatTimer.getDelay() - Constants.BEAT_DELTA));
			alternateBeat = 2;
		}
		// Will alternate beat
		else if (e.getSource() == beatTimer && alternateBeat == 2) {
			Participant.getSounds().playBeat2Clip();
			beatTimer.setDelay(Math.max(Constants.FASTEST_BEAT, beatTimer.getDelay() - Constants.BEAT_DELTA));
			alternateBeat = 1;
		}

		// Time to refresh the screen and deal with keyboard input
		else if (e.getSource() == refreshTimer) {

			// It may be time to make a game transition
			performTransition();

			// Move the participants to their new locations
			pstate.moveParticipants();

			if (turnLeft && ship != null) {
				ship.turnLeft();
			}
			if (turnRight && ship != null) {
				ship.turnRight();
			}
			if (goForward && ship != null) {
				ship.accelerate();
			} else if (ship != null) {
				ship.notAccelerating();
			}
			// Will fire the bullet if the ship is not null and then turn
			// fireBullet to false right away so no continuous firing
			if (fireBullet && ship != null) {
				ship.fire();
				if (!rapidFire) {
					fireBullet = false;
				}

			}

			// duration of current power-up
			if (secondsLeftPowered <= 0) {
				secondsLeftPowered = 0;
			} else {
				secondsLeftPowered -= FRAME_INTERVAL;
			}

			// set multiplier
			calculator -= .01;
			multiplier = 1 + ((int) calculator);
			if (multiplier < 1) {
				multiplier = 1;
				calculator = 0;
			} else if (multiplier > 4 || maxMultiplier) {
				multiplier = 4;
			}
			display.setMultiplier(multiplier);
			if (pwr != null) {
				display.setPowerUpLabel(pwr.getDiscription(), secondsLeftPowered / 1000);
			}

			// Refresh screen
			display.refresh();
		}

		
		
		
		
		
		
		// Timer that flips between triggering a power-up to appear, and a
		// power-up being used to expire.
		else if (e.getSource() == powerUpTimer) {
			if (pwr == null) {
				this.addParticipant(new PowerUp(this, RANDOM.nextInt(5)));
				powerUpTimer.stop();
			} else if (pwr != null) {
				pwr = null;
				powerDown();
				display.setPowerUpLabel("", 0);
				powerUpTimer.stop();
				powerUpTimer.setDelay((1000 * RANDOM.nextInt(7)) + 3000);
				powerUpTimer.start();

			} else {

			}
		}
		// If the Alien Ship is not in play, call an Alien Ship
		if (e.getSource() == alienShipTimer) {
			if (!pstate.isAlienShip()) {
				callAlienShip(level);
				alienShipTimer.stop();
			}
		}
	}

	/**
	 * Returns an iterator over the active participants
	 */
	public Iterator<Participant> getParticipants() {
		return pstate.getParticipants();
	}

	/**
	 * If the transition time has been reached, transition to a new state
	 */
	private void performTransition() {
		// Do something only if the time has been reached
		if (transitionTime <= System.currentTimeMillis()) {
			// Clear the transition time
			transitionTime = Long.MAX_VALUE;

			// If there are no lives left, the game is over. Show the final
			// screen.
			if (lives <= 0) {
				beatTimer.stop();
				finalScreen();
			}

			// If the ship was destroyed, place a new one and continue
			else if (ship == null) {
				placeShip();
			}

			// Moves to next level
			if (pstate.countAsteroids() == 0) {

				level++;
				newLevel(level);
			}
		}
	}

	/**
	 * sets up the game for a new level.
	 */
	private void newLevel(int level) {
		pwr = null;
		powerDown();
		display.setLevel(level);
		display.setLegend("");
		clear();
		placeAsteroids(level);
		placeShip();
		multiplier = 1;
		calculator = 0;
		alienShipTimer.restart();
		beatTimer.stop();
		beatTimer.setDelay(INITIAL_BEAT);
		beatTimer.start();
		powerUpTimer.stop();
		powerUpTimer.setDelay(4000);
		powerUpTimer.start();
	}

	/**
	 * summons an alien ship based on level
	 */
	public void callAlienShip(int level) {
		AlienShip aShip;

		// Will call either a small or medium sized Alien Ship after level 2
		if (level > 2) {
			aShip = new AlienShip(Constants.RANDOM.nextBoolean(), this);
		}
		// Will only call the medium size ship if level 2
		else {
			aShip = new AlienShip(false, this);
		}
		this.addParticipant(aShip);
	}

	/**
	 * If a key of interest is pressed, record that it is down, start a timer
	 * that rotates the ship smoothly.
	 */
	@Override
	public void keyPressed(KeyEvent e) {

		int keyCode = e.getKeyCode();
		// Rotate the ship left
		if ((keyCode == KeyEvent.VK_A || keyCode == KeyEvent.VK_LEFT) && ship != null) {
			turnLeft = true;
		}
		// Rotate the ship right
		if ((keyCode == KeyEvent.VK_D || keyCode == KeyEvent.VK_RIGHT) && ship != null) {
			turnRight = true;
		}
		// Accelerate the ship forward
		if ((keyCode == KeyEvent.VK_W || keyCode == KeyEvent.VK_UP) && ship != null) {
			goForward = true;
		}
		// Fires the bullet
		if ((keyCode == KeyEvent.VK_S || keyCode == KeyEvent.VK_SPACE) && ship != null) {
			fireBullet = true;
		}

		// Shoots special bullets.
		// Comment/Remove the if statement below, play game, hold space bar
		// down, spin in a circle, BOOM, you win every time.
		if (keyCode == KeyEvent.VK_1) {
			if (bigGunLoaded) {
				this.getShip().fireBigGun();
				bigGunLoaded = false;
				display.setPowerUpLabel("Hot Shot Bullet Used", 0);
			}
		}
	}

	/**
	 * Ignore these events.
	 */
	@Override
	public void keyTyped(KeyEvent e) {
	}

	/**
	 * Stops the timer that rotates the ship.
	 */
	@Override
	public void keyReleased(KeyEvent e) {
		int keyCode = e.getKeyCode();
		if ((keyCode == KeyEvent.VK_A || keyCode == KeyEvent.VK_LEFT) && ship != null) {
			turnLeft = false;
		}
		if ((keyCode == KeyEvent.VK_D || keyCode == KeyEvent.VK_RIGHT) && ship != null) {
			turnRight = false;
		}
		if ((keyCode == KeyEvent.VK_W || keyCode == KeyEvent.VK_UP) && ship != null) {
			goForward = false;
		}
		if ((keyCode == KeyEvent.VK_S || keyCode == KeyEvent.VK_SPACE) && ship != null) {
			if (rapidFire) {
			} else {
				fireBullet = false;
			}
		}
	}

	// Keeps checking if there are more than 8 bullets on the screen.
	// if so, it'll expire 1 bullet, keeping only a maximum of 8 bullets
	// on screen.
	public boolean bulletLimit(int bulletAmount) {
		if (pstate.countBullets() >= bulletAmount) {
			return true;
		}
		return false;
	}

	/**
	 * Increments the score
	 */
	public void scoreAdder(int n) {
		score += n * multiplier;
		display.setScore(score);
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	/**
	 * @return if the bigGunLoaded or not
	 */
	public boolean isBigGunLoaded() {
		return bigGunLoaded;
	}

	/**
	 * @param bigGunLoaded
	 *            the bigGunLoaded to set
	 */
	public void setBigGunLoaded(boolean bigGunLoaded) {
		this.bigGunLoaded = bigGunLoaded;
	}

	/**
	 * @return the invincible
	 */
	public boolean isInvincible() {
		return invincible;
	}

	/**
	 * Adds to the score if you destroy the ship. no matter what destroys the
	 * ship restarts the call ship timer.
	 */
	public void etGoneHome(int worth) {
		calculator += .5;
		scoreAdder(worth);
		alienShipTimer.start();

	}

	/**
	 * @return the multiplier
	 */
	public int getMultiplier() {
		return multiplier;
	}

	/**
	 * takes the power-ups off
	 */
	public void powerDown() {
		rapidFire = false;
		invincible = false;
		maxMultiplier = false;
	}

	/**
	 * lets a power-up take effect.
	 */
	public void powerUpEngage(PowerUp powerUp) {
		pwr = powerUp;
		if (pwr.getDuration() != 0) {
			powerUpTimer.stop();
			powerUpTimer.setDelay(pwr.getDuration());
			secondsLeftPowered = pwr.getDuration();
			powerUpTimer.restart();
			switch (pwr.getIndex()) {
			case 0:
				rapidFire = true;
				break;
			case 2:
				maxMultiplier = true;
				break;
			case 4:
				invincible = true;
				break;

			}
		} else {
			switch (pwr.getIndex()) {
			case 1:
				// extra life power-up
				lives++;
				display.setLives(lives);
				display.setPowerUpLabel(pwr.getDiscription(), 0);
				break;
			case 3:
				// enables a big shot
				bigGunLoaded = true;
				display.setPowerUpLabel("Hot Shot Ready | Bullets left: 1 | Press 1 to shoot.", 0);
				break;
			}

			pwr = null;
			powerUpTimer.stop();
			powerUpTimer.setDelay((1000 * RANDOM.nextInt(7)) + 3000);
			powerUpTimer.start();
		}

	}	

	/**
	 * 
	 * @return whether or not the ship is rapid firing.
	 */
	public boolean isRapidFire() {
		return rapidFire;
	}

}
