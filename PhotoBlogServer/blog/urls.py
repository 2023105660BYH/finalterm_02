from django.urls import path,include
from . import views
from rest_framework import routers
from rest_framework_simplejwt.views import TokenObtainPairView
from .views import PostListCreateView



urlpatterns = [
    path('', views.post_list, name='post_list'), 
    path('post/<int:pk>/', views.post_detail, name='post_detail'),
    path('post/new/', views.post_new, name='post_new'),
    path('post/<int:pk>/edit/', views.post_edit, name='post_edit'),
    path('api_root/Post/', PostListCreateView.as_view(), name='post-create'),
]