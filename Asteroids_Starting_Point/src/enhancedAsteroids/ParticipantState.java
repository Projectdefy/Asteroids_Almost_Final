package enhancedAsteroids;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import enhancedAsteroids.destroyers.AlienShip;
import enhancedAsteroids.participants.Asteroid;
import enhancedAsteroids.participants.FriendlyBullet;

/**
 * Keeps track of the Participants, their motions, and
 * their collisions.
 */
public class ParticipantState
{
    // The participants (asteroids, ships, etc.) that are involved in the game
    private LinkedList<Participant> participants;

    // Participants that are waiting to be added to the game
    private Set<Participant> pendingAdds;
    
    /**
     * Creates an empty ParticipantState.
     */
    public ParticipantState ()
    {
        // No participants at the start
        participants = new LinkedList<Participant>();
        pendingAdds = new HashSet<Participant>();
    }
    
    /**
     * Clears out the state.
     */
    public void clear ()
    {
        pendingAdds.clear();
        for (Participant p : participants)
        {
            Participant.expire(p);
        }
        participants.clear();
    }  

    /**
     * Adds a new Participant
     */
    public void addParticipant (Participant p)
    {
        pendingAdds.add(p);
    }
    
    /**
     * Returns an iterator over the active participants
     */
    public Iterator<Participant> getParticipants ()
    {
        return participants.iterator();
    }

    /**
     * Returns the number of asteroids that are active participants
     */
    public int countAsteroids ()
    {
        int count = 0;
        for (Participant p : participants)
        {
            if (p instanceof Asteroid && !p.isExpired())
            {
                count++;
            }
        }
        for (Participant p : pendingAdds)
        {
            if (p instanceof Asteroid && !p.isExpired())
            {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Moves each of the active participants to simulate the passage of time.
     */
    public void moveParticipants ()
    {
        // Move all of the active participants
        for (Participant p : participants)
        {
            if (!p.isExpired())
            {
                p.move();
            }
        }

        // If there have been any collisions, deal with them. This may result
        // in new participants being added or old ones expiring. We save those
        // changes until after all of the collisions have been processed.
        checkForCollisions();

        // Deal with pending adds and expirations
        completeAddsAndRemoves();
    }

    /**
     * Completes any adds and removes that have been requested.
     */
    private void completeAddsAndRemoves ()
    {
        // Note: These updates are saved up and done later to avoid modifying
        // the participants list while it is being iterated over
        for (Participant p : pendingAdds)
        {
            participants.add(p);
        }
        pendingAdds.clear();

        Iterator<Participant> iter = participants.iterator();
        while (iter.hasNext())
        {
            Participant p = iter.next();
            if (p.isExpired())
            {
                iter.remove();
            }
        }
    }
    

    /**
     * Compares each pair of elements to detect collisions, then notifies all
     * listeners of any found. Deals with each pair only once. Never deals with
     * (p1,p2) and then again with (p2,p1).
     */
    private void checkForCollisions ()
    {
        for (Participant p1 : participants)
        {
            if (!p1.isExpired())
            {
                Iterator<Participant> iter = participants.descendingIterator();
                while (iter.hasNext())
                {
                    Participant p2 = iter.next();
                    if (p1 == p2)
                        break;
                    if (!p2.isExpired() && p1.overlaps(p2))
                    {
                        p1.collidedWith(p2);
                        p2.collidedWith(p1);
                    }
                    if (p1.isExpired())
                        break;
                }
            }
        }
    }
    /**
     * returns true if there is an alien ship in play
     */
    public boolean isAlienShip(){
    	for(Participant p : participants){
    		if(!p.isExpired() && p instanceof AlienShip){
    			return true;
    		}
    	}
    	for(Participant p : pendingAdds){
    		if(!p.isExpired() && p instanceof AlienShip){
    			return true;
    		}
    	}
    	return false;
    }
    /**
     * Counts how many bullets are on screen.
     */
    public int countBullets ()
    {
    	int nBullets = 0;
    	for(Participant p : participants){
    		if((p instanceof FriendlyBullet) && !p.isExpired())
    		{
    			nBullets++;
    		}
    		
    	}
    	for (Participant p : pendingAdds) {
            if ((p instanceof FriendlyBullet) && !p.isExpired())
            nBullets++;
        }
    	return nBullets;
    }
}
