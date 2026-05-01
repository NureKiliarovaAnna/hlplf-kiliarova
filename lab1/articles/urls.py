from django.urls import path
from . import views

urlpatterns = [
    path('', views.article_list, name='article_list'),
    path('article/<int:pk>/', views.article_detail, name='article_detail'),
    path('author/<int:author_id>/', views.articles_by_author, name='articles_by_author'),
    path('api/articles/', views.article_api_list, name='article_api_list'),
]