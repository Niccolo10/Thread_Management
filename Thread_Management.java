
package esame2206;

import java.util.ArrayList;

class Image{

    public int p;
    public Object image;

    public Image(int p) {
        this.p = p;
        Object image = new Object();
    }

}

class Veicolo extends Thread{

    private float x,y;
    private float [] position = new float[2];
    public volatile ArrayList<Image> data = new ArrayList<Image>();
    private LocationTracker lc;
    int commutazioni=0;

    public Veicolo(LocationTracker lc){
        this.lc = lc;
        float x = (float)(Math.random()*20-10);
        float y = (float)(Math.random()*20-10);
        position[0] = x;
        position[1] = y;
    }


    public void run (){
        try{
            while(true){
                Image image = getImage();
                putData(image);
                move();
                sleep(1000);
            }
        }catch (InterruptedException e){
            System.err.println(getName()+" Interrotto");
        }
    }


    private Image getImage(){

        Image image = new Image(lc.getPriority(position));
        System.out.println(getName() + " Nuova immagine acquisita");
        System.out.println("Priorità di "+ getName()+ " assegnata");
        return image;

    }

    private void putData(Image image) throws InterruptedException {
        boolean find = false;

        for (int i = 0; i <data.size(); i++) {
            if (data.get(i).p > image.p) {
                data.add(i,image);
                find = true;
                break;
            }
        }
        if(!find)
            data.add(image);
        System.out.println(getName() + " Immagine aggiunta alla coda locale");

    }


    private void move(){
        position[0] = position[0] - (float)(Math.random()*2-1);
        position[1] = position[1] - (float)(Math.random()*2-1);
        commutazioni++;
        System.out.println("Posizione di "+ getName()+ "aggiornata");
    }
}

class LocationTracker{

    int cposizione =0;
    int p1=0;
    int p2=0;
    int p3=0;

    public synchronized int getPriority(float[] position){

        cposizione ++;
        float distance = (float)Math.sqrt(Math.pow(position[0], 2) + (Math.pow(position[1], 2)));                                                                                    //////////////////
        if (distance < 5){
            int p=1;
            notifyAll();
            p1++;
            return p;
        }
        else if  (distance < 10){
            int p =2;
            notifyAll();
            p2++;
            return p;
        }
        else{
            int p=3;
            notifyAll();
            p3++;
            return p;
        }

    }

    public void getNumberVeicoli(){

        System.out.println("------------------------------------------------------");
        System.out.println("Priorità 1->"+p1+"/Priorità 2->"+p2+"/Priorità 3->"+p3);

        p1=0;
        p2=0;
        p3=0;

    }
}

class Uploader extends Thread{

    private Veicolo v;
    private Queue q;

    public Uploader(Veicolo v, Queue q) {
        this.v = v;
        this.q = q;
    }

    public void run (){

        try{
            while(true) {
                while (v.data.size()==0){}
                q.add(v.data.remove(0));
                System.out.println(getName()+ "immagine rimossa dalla coda locale");
            }
        }catch(InterruptedException e){
            System.err.println(getName() + " Interrotto");
        }
    }
}

class Collector extends Thread{
    private Queue q;

    public Collector(Queue q) {
        this.q = q;
    }

    public synchronized void run() {
        try{
            while (true){
                q.get();
                System.out.println(getName()+ "Imagine presa dalla coda globale");
                sleep(1000);
            }
        }catch (InterruptedException e){
            System.err.println(getName()+" Interrotto");
        }
    }
}

class Queue{
    int k;
    public ArrayList<Image> data;

    public Queue(int k) {
        this.k = k;
        data = new ArrayList<Image>(k);
    }

    public synchronized void add(Image o) throws InterruptedException {
        while (isFull())
            wait();
        data.add(o);
        notifyAll();
    }

    private boolean isFull(){
        if(data.size() < k)
            return false;
        else
            return true;
    }

    public synchronized void get() throws InterruptedException {
        while (data.isEmpty())
            wait();
        data.remove(0);
        notifyAll();
    }

    public synchronized int getSize(){
        int x= data.size();
        notifyAll();
        return x;
    }
}


public class Esame2206 {
    public static void main(String[] args) throws InterruptedException {

        LocationTracker lc = new LocationTracker();
        int n = 10;
        int m = 3 ;
        Queue q = new Queue(20);
        Veicolo [] v = new Veicolo[n];
        Uploader [] u = new Uploader[n];
        Collector [] c = new Collector[m];

        for (int i=0; i<v.length; i++){
            v[i] = new Veicolo(lc);
            v[i].setName("V_"+i);
            v[i].start();
        }

        for (int i=0; i<u.length; i++){
            u[i] = new Uploader(v[i],q);
            u[i].setName("U_"+i);
            u[i].start();
        }

        for (int i=0; i<c.length; i++){
            c[i] = new Collector(q);
            c[i].setName("C_"+i);
            c[i].start();
        }

        int time=0;
        while (time != 30) {

            Thread.sleep(1000);
            lc.getNumberVeicoli();
            System.out.println("Message queue " + q.getSize());
            System.out.println("------------------------------------------------------");
            time++;
        }

        int tc=0;

        for(Uploader uploader : u){ uploader.interrupt(); }

        for (Collector collector : c) collector.interrupt();

        for (Veicolo veicolo : v) {
            veicolo.interrupt();
            veicolo.join();
            tc = tc + veicolo.commutazioni;
        }

        System.out.println("Commutazioni eseguite->"+ tc+ " / Cambiamenti di posizione acquisiti->"+ lc.cposizione);
    }
}

