package com.example.sardapp.api.dao;

import com.example.sardapp.api.session.AbstractSession;
import com.example.sardapp.entities.Intent;
import com.google.api.services.dialogflow.v2.model.GoogleCloudDialogflowV2Context;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.springframework.stereotype.Repository;

import javax.persistence.criteria.CriteriaBuilder;
import java.util.List;

@Repository
public class IntentDAOImpl extends AbstractSession implements IntentDAO
{

    void clearSession() {
        close();
    }

    @Override
    public boolean save(Intent intent) {
        getSession().beginTransaction();
        getSession().saveOrUpdate(intent);
        getSession().getTransaction().commit();
        return getSession().getTransaction().getStatus() == TransactionStatus.COMMITTED;
    }

    @Override
    public Intent findById(int numero) {
        return getSession().get(Intent.class, numero);
    }

    @Override
    public List<Intent> findAll() {
        return getSession().createQuery("select i from Intent i ").list();
    }

    @Override
    public void pushStackContext(String textResponse, String intentName) {
        Intent intent = new Intent();
        intent.setResponse(textResponse);

        intent.setIntent(intentName);

        Intent topIntent = this.topStack();
        int numero = 1;
        if(topIntent != null) numero = topIntent.getNumero() + 1;
        intent.setNumero(numero);

        boolean correct = this.save(intent);
    }

    @Override
    public Intent topStack() {
        List<Intent> intents = getSession().createQuery("SELECT i " +
                                        "FROM Intent i " +
                                        "where i.numero = (SELECT MAX(i2.numero) FROM Intent i2)").list();
/*
        DetachedCriteria maxId = DetachedCriteria.forClass(Intent.class)
                .setProjection( Projections.max("id") );
        getSession().createCriteria(Intent.class)
                .add( Property.forName("id").eq(maxId) )
                .list();

        CriteriaBuilder cb = getSession().getCriteriaBuilder();
        cb.max*/


        if(intents==null || intents.size() == 0) return null;
        else return intents.get(0);
    }

    @Override
    public void popStackContext(Intent stackElement) {
        getSession().beginTransaction();
        /*getSession().createSQLQuery("DELETE FROM intents i " +
                                        "WHERE i.numero = (SELECT MAX(i2.numero) FROM Intent i2)").executeUpdate();
        */
        getSession().delete(stackElement);

        getSession().getTransaction().commit();
        boolean correct =  getSession().getTransaction().getStatus() == TransactionStatus.COMMITTED;
        getSession().clear();
    }



    private String parseOutputContexts(List<GoogleCloudDialogflowV2Context> outputContexts) {
        StringBuilder result = new StringBuilder();
        if(outputContexts.size() > 0) result.append(outputContexts.get(0).getName());

        String contextNameZero = outputContexts.get(0).getName();
        System.out.println("contextName first one = " + contextNameZero);

        for(int i = 1; i < outputContexts.size(); ++i) {
            GoogleCloudDialogflowV2Context outContext = outputContexts.get(i);
            String contextName = outContext.getName();
            System.out.println("contextName = " + contextName);
            result.append(";").append(contextName);
        }
        return result.toString();
    }


    @Override
    public void setUpDB(String intentName, String textResponse) {
        try {
            getSession().beginTransaction();
            getSession().createSQLQuery("CREATE TABLE IF NOT EXISTS intents (numero integer, " +
                    "response varchar(8191), " +
                    "intent varchar(8191))").executeUpdate();
            getSession().createSQLQuery("DELETE FROM intents").executeUpdate();
            getSession().clear();

            Intent intent = new Intent();
            intent.setNumero(1);
            intent.setIntent(intentName);
            intent.setResponse(textResponse);

            getSession().saveOrUpdate(intent);
            getSession().getTransaction().commit();
            boolean correct = getSession().getTransaction().getStatus() == TransactionStatus.COMMITTED;
            //getSession().clear();
        }
        catch (Exception e) {
            e.printStackTrace();
            getSession().getTransaction().rollback();
            getSession().clear();
        }
    }

    @Override
    public String setUpEmptyDB() {
        try {
            getSession().beginTransaction();
            getSession().createSQLQuery("CREATE TABLE IF NOT EXISTS intents (numero integer, " +
                    "response varchar(8191), " +
                    "intent varchar(8191))").executeUpdate();
            getSession().createSQLQuery("DELETE FROM intents").executeUpdate();

            getSession().getTransaction().commit();
            boolean correct = getSession().getTransaction().getStatus() == TransactionStatus.COMMITTED;
            getSession().clear();

            return correct ? "Committed correctly" : "Something went wrong";
        }
        catch (Exception e) {
            e.printStackTrace();
            getSession().getTransaction().rollback();
            getSession().clear();

            return "Database transaction cannot commit";
        }
    }


}
