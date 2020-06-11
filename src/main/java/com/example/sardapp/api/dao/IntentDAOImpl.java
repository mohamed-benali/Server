package com.example.sardapp.api.dao;

import com.example.sardapp.api.session.AbstractSession;
import com.example.sardapp.entities.Intent;
import com.google.api.services.dialogflow.v2.model.GoogleCloudDialogflowV2Context;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class IntentDAOImpl extends AbstractSession implements IntentDAO
{


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
    public void pushStackContext(String textResponse, List<GoogleCloudDialogflowV2Context> outputContexts) {
        Intent intent = new Intent();
        intent.setResponse(textResponse);

        String outContexts = this.parseOutputContexts(outputContexts);
        intent.setOutcontext(outContexts);

        Intent topIntent = this.topStack();
        int numero = 1;
        if(topIntent != null) numero = topIntent.getNumero() + 1;
        intent.setNumero(numero);

        boolean correct = this.save(intent);
    }

    @Override
    public Intent topStack() {
        List<Intent> intent = getSession().createSQLQuery("SELECT i " +
                                        "FROM Intent i " +
                                        "WHERE i.numero = (SELECT MAX(i2.numero) FROM Intent i2)").list();

        if(intent==null || intent.size() == 0) return null;
        else return intent.get(0);
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
    }



    private String parseOutputContexts(List<GoogleCloudDialogflowV2Context> outputContexts) {
        StringBuilder result = new StringBuilder();
        if(outputContexts.size() > 0) result.append(outputContexts.get(0));
        for(int i = 1; i < outputContexts.size(); ++i) {
            GoogleCloudDialogflowV2Context outContext = outputContexts.get(i);
            result.append(";").append(outContext.getName());
        }
        return result.toString();
    }


    @Override
    public void setUpDB(List<GoogleCloudDialogflowV2Context> outputContexts, String textResponse) {
        getSession().beginTransaction();
        getSession().createSQLQuery("CREATE TABLE IF NOT EXISTS intents (numero int, " +
                "response varchar(255), " +
                "outcontext varchar(255))").executeUpdate();
        getSession().createSQLQuery("DELETE FROM intents").executeUpdate();

        Intent intent = new Intent();
        intent.setNumero(1);
        intent.setOutcontext(parseOutputContexts(outputContexts));
        intent.setResponse(textResponse);

        getSession().saveOrUpdate(intent);
        getSession().getTransaction().commit();
        boolean correct =  getSession().getTransaction().getStatus() == TransactionStatus.COMMITTED;
    }

    @Override
    public String setUpEmptyDB() {
        try {
            getSession().beginTransaction();
            getSession().createSQLQuery("CREATE TABLE IF NOT EXISTS intents (numero integer, " +
                    "response varchar(255), " +
                    "outcontext varchar(255))").executeUpdate();
            getSession().createSQLQuery("DELETE FROM intents").executeUpdate();

            getSession().getTransaction().commit();
            boolean correct = getSession().getTransaction().getStatus() == TransactionStatus.COMMITTED;

            return correct ? "Committed correctly" : "Something went wrong";
        }
        catch (Exception e) {
            e.printStackTrace();
            getSession().getTransaction().rollback();
            return "Database transaction cannot commit";
        }
    }

}
