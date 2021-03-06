package cs131.pa1.filter.concurrent;

import cs131.pa1.filter.Message;

import java.util.*;


public class ConcurrentREPL {

	static String currentWorkingDirectory;
	//a list that stores all background command objects
	static List<BackgroundCommand> backgroundJobs = new ArrayList<>(); 
	static int id; 
	static String command;
	
	
	public static void main(String[] args){
		currentWorkingDirectory = System.getProperty("user.dir");
		Scanner s = new Scanner(System.in);
		System.out.print(Message.WELCOME);
		backgroundJobs = new ArrayList<>(); 
		id = 1; 
		
		while(true) {
			//obtaining the command from the user
			System.out.print(Message.NEWCOMMAND);
			command = s.nextLine().trim();
			
			if(command.equals("exit")) {
				break;
			//if if command is not empty
			} else if(!command.equals("")) {
				
				//user wants jobs displayed
				if (command.equals("repl_jobs")) {
					displayJobs();
					
				//user wants to kill a certain background job
				} else if (command.split(" ")[0].equals("kill")) {
					
					//before proceeding any further, check that parameter is valid/exists
					if (command.split(" ").length == 1) {
						System.out.printf(Message.REQUIRES_PARAMETER.toString(), command);
					} else if (!Character.isDigit(command.split(" ")[1].charAt(0))) {
						System.out.printf(Message.INVALID_PARAMETER.toString(), command);
					} else {
						//proceed to kill command by first obtaining the index 
						//of the background job to kill
						int index = command.charAt(command.length()-1)-'0';
						//counter to make sure we kill the right job
						int i = 1;
						//go through the background command list
						for(BackgroundCommand job: backgroundJobs) {
							//delete the job from the list if its id matches user input index
							if (index == i) {
								//interrupt thread. Deal with killing it in redirect or print
								job.getThread().interrupt();
								backgroundJobs.remove(job);
								break;
							}
							i++;
						}
					}
				} else {
					//building the filters list from the command
					List<ConcurrentFilter> filterlist = ConcurrentCommandBuilder.createFiltersFromCommand(command);
					
					if (filterlist != null) {
						if (command.endsWith("&")) {
							createBackThreadExecuteFilters(filterlist);
						} else {
							createThreadExecuteFilters(filterlist);
						}	
					}
				}
			}
		}
		System.out.print(Message.GOODBYE);
	}
	
	/**
	 * This method creates threads to execute normal commands (not background)
	 * @param filterlist
	 */
	public static void createThreadExecuteFilters(List<ConcurrentFilter> filterlist) {
		//Creating threads from the filter list and starting all threads
		Thread thr = new Thread();
		for (ConcurrentFilter filter: filterlist) {
			thr = new Thread(filter);
			thr.start();
		}
		
		//main waiting for each child thread to complete before regaining control
		try {
			thr.join();
		} catch (InterruptedException e){
			e.printStackTrace();
		}
	}
	
	/**
	 * This method creates and run threads for background jobs
	 * @param filterlist
	 */
	public static void createBackThreadExecuteFilters(List<ConcurrentFilter> filterlist) {
		//Creating threads from the filter list and starting all threads
		Thread thr = new Thread();
		for (ConcurrentFilter filter: filterlist) {
			thr = new Thread(filter);
			thr.start();
		}
		//using last thread, create background command object and add it to list
		backgroundJobs.add(new BackgroundCommand(id,command,thr));
		id++;

	}
	
	//prints out background jobs line by line
	public static void displayJobs() {
		for (Iterator<BackgroundCommand> it = backgroundJobs.iterator(); it.hasNext(); ) {
			//create an iterator
		    BackgroundCommand job = it.next();
		    //deletes the job from the list if its thread is terminated
		    if (!job.getThread().isAlive()) {
		        it.remove();		    
		    } else {
		    	//print the command string out if it's still running
		    	System.out.println("\t"+ job.getId() + "." +" "+job.getCommand());
		    }
		}
	}
}
