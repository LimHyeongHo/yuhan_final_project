package com.demo.gp.dto;

import com.demo.gp.entity.Participation;
import java.util.List;
import java.util.Map;

public class DeadlineResult {

    private Integer gpId;
    private String title;
    private String scenario;
    private String finalStatus;

    private List<String> failedUsers;
    private int beforeQty;
    private int afterQty;
    private int beforePrice;
    private int afterPrice;
    private int extraPayment;           // ✅ 추가: 추가 결제금

    private boolean minQtyFailed;
    private List<String> refundedUsers;

    private List<Participation> allParticipations;
    private Map<Integer, com.demo.gp.entity.User> buyerMap;

    public Integer getGpId()                                  { return gpId; }
    public void setGpId(Integer gpId)                         { this.gpId = gpId; }

    public String getTitle()                                  { return title; }
    public void setTitle(String title)                        { this.title = title; }

    public String getScenario()                               { return scenario; }
    public void setScenario(String scenario)                  { this.scenario = scenario; }

    public String getFinalStatus()                            { return finalStatus; }
    public void setFinalStatus(String s)                      { this.finalStatus = s; }

    public List<String> getFailedUsers()                      { return failedUsers; }
    public void setFailedUsers(List<String> list)             { this.failedUsers = list; }

    public int getBeforeQty()                                 { return beforeQty; }
    public void setBeforeQty(int qty)                         { this.beforeQty = qty; }

    public int getAfterQty()                                  { return afterQty; }
    public void setAfterQty(int qty)                          { this.afterQty = qty; }

    public int getBeforePrice()                               { return beforePrice; }
    public void setBeforePrice(int p)                         { this.beforePrice = p; }

    public int getAfterPrice()                                { return afterPrice; }
    public void setAfterPrice(int p)                          { this.afterPrice = p; }

    public int getExtraPayment()                              { return extraPayment; }
    public void setExtraPayment(int extra)                    { this.extraPayment = extra; }

    public boolean isMinQtyFailed()                           { return minQtyFailed; }
    public void setMinQtyFailed(boolean b)                    { this.minQtyFailed = b; }

    public List<String> getRefundedUsers()                    { return refundedUsers; }
    public void setRefundedUsers(List<String> list)           { this.refundedUsers = list; }

    public List<Participation> getAllParticipations()          { return allParticipations; }
    public void setAllParticipations(List<Participation> l)   { this.allParticipations = l; }

    public Map<Integer, com.demo.gp.entity.User> getBuyerMap()        { return buyerMap; }
    public void setBuyerMap(Map<Integer, com.demo.gp.entity.User> m)  { this.buyerMap = m; }
}